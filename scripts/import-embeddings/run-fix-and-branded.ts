/**
 * run-fix-and-branded.ts
 *
 * 1. Fix Foundation Foods: re-import with correct kcal number ("957" not "208")
 * 2. Import Branded Foods (~400K items) — runs for hours, restart-safe via upsert.
 *
 * Usage:
 *   deno run --allow-net --allow-env --env=.env run-fix-and-branded.ts
 */

import { embedBatched } from "./_lib/openai.ts";
import { upsertBatch } from "./_lib/supabase.ts";

const EMBED_BATCH = 100;
const USDA_PAGE_SIZE = 200;
const USDA_SAFETY_PAGE_LIMIT = 2500; // Branded has ~400K items = ~2000 pages

// Foundation Foods use "957" (Atwater General Factors) instead of "208" for kcal.
const NUTRIENT_STANDARD = {
  kcal: "208",
  protein: "203",
  fat: "204",
  carbs: "205",
  fiber: "291",
  sugar: "269",
};

const NUTRIENT_FOUNDATION = {
  ...NUTRIENT_STANDARD,
  kcal: "957", // Energy (Atwater General Factors)
};

const USDA_API_KEY = Deno.env.get("USDA_API_KEY");
const FDC_BASE = "https://api.nal.usda.gov/fdc/v1";

interface FdcFood {
  fdcId: number;
  description: string;
  foodNutrients?: { number?: string; amount?: number; value?: number }[];
}

async function fetchPage(dataType: string, page: number): Promise<FdcFood[]> {
  const encodedType = encodeURIComponent(dataType);
  for (let attempt = 0; attempt < 4; attempt++) {
    const url =
      `${FDC_BASE}/foods/list?dataType=${encodedType}&pageSize=${USDA_PAGE_SIZE}&pageNumber=${page}&api_key=${USDA_API_KEY}`;
    const res = await fetch(url);
    if (res.status === 429) {
      const delay = Math.pow(2, attempt) * 2000;
      console.warn(`    Rate limit — retry in ${delay}ms`);
      await new Promise((r) => setTimeout(r, delay));
      continue;
    }
    if (res.status === 400) {
      console.warn(`    Bad request on page ${page} — likely past last page`);
      return [];
    }
    if (!res.ok) throw new Error(`FDC API ${res.status}: ${await res.text()}`);
    const json = await res.json();
    if (Array.isArray(json)) return json as FdcFood[];
    if (Array.isArray(json.foods)) return json.foods as FdcFood[];
    throw new Error(`Unexpected FDC shape: ${JSON.stringify(json).slice(0, 200)}`);
  }
  throw new Error(`FDC failed after retries on page ${page}`);
}

function nutrientValue(
  food: FdcFood,
  number: string,
): number | null {
  const found = food.foodNutrients?.find((n) => n.number === number);
  if (!found) return null;
  return (found.amount ?? found.value) ?? null;
}

function normalize(name: string): string {
  return name.toLowerCase().trim().replace(/\s+/g, " ");
}

interface DatasetConfig {
  label: string;
  fdcDataType: string;
  source: string;
  nutrients: Record<string, string>;
}

async function importDataset(cfg: DatasetConfig): Promise<number> {
  console.log(`\n=== ${cfg.label} ===`);
  if (!USDA_API_KEY) {
    console.warn("  USDA_API_KEY not set — skipping.");
    return 0;
  }

  let total = 0;
  let page = 1;

  while (page <= USDA_SAFETY_PAGE_LIMIT) {
    let foods: FdcFood[];
    try {
      foods = await fetchPage(cfg.fdcDataType, page);
    } catch (err) {
      console.error(`  Page ${page} failed:`, err);
      break;
    }

    if (foods.length === 0) {
      console.log(`  Page ${page} empty — stopping.`);
      break;
    }

    const names = foods.map((f) => f.description);
    const embeddings = await embedBatched(names, EMBED_BATCH);

    const rows = foods.map((food, i) => ({
      name: food.description,
      name_normalized: normalize(food.description),
      source: cfg.source,
      ext_id: String(food.fdcId),
      fdc_id: food.fdcId,
      protein: nutrientValue(food, cfg.nutrients.protein) ?? 0,
      calories: nutrientValue(food, cfg.nutrients.kcal) ?? 0,
      fat: nutrientValue(food, cfg.nutrients.fat),
      carbs: nutrientValue(food, cfg.nutrients.carbs),
      fiber_100g: nutrientValue(food, cfg.nutrients.fiber),
      sugar_100g: nutrientValue(food, cfg.nutrients.sugar),
      default_portion_grams: 100,
      is_active: true,
      popularity: 0,
      embedding: `[${embeddings[i].join(",")}]`,
    }));

    const CHUNK = 10;
    for (let c = 0; c < rows.length; c += CHUNK) {
      await upsertBatch("food_catalog", rows.slice(c, c + CHUNK), "source,ext_id", true);
    }
    total += foods.length;
    console.log(`  Page ${page} — ${foods.length} items, total: ${total}`);

    if (foods.length < USDA_PAGE_SIZE) {
      console.log("  Last page.");
      break;
    }

    await new Promise((r) => setTimeout(r, 250));
    page++;
  }

  console.log(`\n  Done: ${total} items.`);
  return total;
}

async function main() {
  const start = Date.now();
  console.log("=== Fix Foundation Foods + Import Branded ===\n");

  // 1. Re-import Foundation with correct kcal number (merge-duplicates to update)
  const foundationCount = await importDataset({
    label: "Foundation Foods (fix kcal → 957)",
    fdcDataType: "Foundation",
    source: "usda_foundation",
    nutrients: NUTRIENT_FOUNDATION,
  });

  // 2. Import Branded (massive dataset)
  const brandedCount = await importDataset({
    label: "Branded Foods",
    fdcDataType: "Branded",
    source: "usda_branded",
    nutrients: NUTRIENT_STANDARD,
  });

  const elapsed = Math.round((Date.now() - start) / 1000);
  const hours = Math.floor(elapsed / 3600);
  const mins = Math.floor((elapsed % 3600) / 60);
  console.log("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  console.log(`Done in ${hours}h${mins}m`);
  console.log(`   • Foundation Foods (fixed): ${foundationCount}`);
  console.log(`   • Branded Foods:            ${brandedCount}`);
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
}

main().catch((e) => {
  console.error("Failed:", e);
  Deno.exit(1);
});
