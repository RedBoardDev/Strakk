/**
 * run-usda-extra.ts — Import Foundation Foods + Survey FNDDS only.
 * Run this once to supplement the existing SR Legacy dataset.
 *
 * Usage:
 *   deno run --allow-net --allow-env --env=.env run-usda-extra.ts
 */

import { embedBatched } from "./_lib/openai.ts";
import { upsertBatch } from "./_lib/supabase.ts";

const EMBED_BATCH = 100;
const USDA_PAGE_SIZE = 200;
const USDA_SAFETY_PAGE_LIMIT = 60;

const NUTRIENT = {
  kcal: "208",
  protein: "203",
  fat: "204",
  carbs: "205",
  fiber: "291",
  sugar: "269",
};

const USDA_API_KEY = Deno.env.get("USDA_API_KEY");
const FDC_BASE = "https://api.nal.usda.gov/fdc/v1";

interface FdcFood {
  fdcId: number;
  description: string;
  foodNutrients?: { number?: string; amount?: number; value?: number }[];
}

async function fetchUsdaPage(dataType: string, page: number): Promise<FdcFood[]> {
  const encodedType = encodeURIComponent(dataType);
  for (let attempt = 0; attempt < 4; attempt++) {
    const url =
      `${FDC_BASE}/foods/list?dataType=${encodedType}&pageSize=${USDA_PAGE_SIZE}&pageNumber=${page}&api_key=${USDA_API_KEY}`;
    const res = await fetch(url);
    if (res.status === 429) {
      const delay = Math.pow(2, attempt) * 2000;
      console.warn(`    FDC rate limit — retry in ${delay}ms`);
      await new Promise((r) => setTimeout(r, delay));
      continue;
    }
    if (!res.ok) throw new Error(`FDC API error ${res.status}: ${await res.text()}`);
    const json = await res.json();
    if (Array.isArray(json)) return json as FdcFood[];
    if (Array.isArray(json.foods)) return json.foods as FdcFood[];
    throw new Error(`Unexpected FDC shape: ${JSON.stringify(json).slice(0, 200)}`);
  }
  throw new Error(`FDC failed after retries on page ${page}`);
}

function nutrientValue(food: FdcFood, number: string): number | null {
  const found = food.foodNutrients?.find((n) => n.number === number);
  if (!found) return null;
  return (found.amount ?? found.value) ?? null;
}

function normalize(name: string): string {
  return name.toLowerCase().trim().replace(/\s+/g, " ");
}

async function importDataset(label: string, fdcDataType: string, source: string): Promise<number> {
  console.log(`\n=== ${label} ===`);
  if (!USDA_API_KEY) {
    console.warn("  USDA_API_KEY not set — skipping.");
    return 0;
  }

  let total = 0;
  let page = 1;

  while (page <= USDA_SAFETY_PAGE_LIMIT) {
    let foods: FdcFood[];
    try {
      foods = await fetchUsdaPage(fdcDataType, page);
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
      source,
      ext_id: String(food.fdcId),
      fdc_id: food.fdcId,
      protein: nutrientValue(food, NUTRIENT.protein) ?? 0,
      calories: nutrientValue(food, NUTRIENT.kcal) ?? 0,
      fat: nutrientValue(food, NUTRIENT.fat),
      carbs: nutrientValue(food, NUTRIENT.carbs),
      fiber_100g: nutrientValue(food, NUTRIENT.fiber),
      sugar_100g: nutrientValue(food, NUTRIENT.sugar),
      default_portion_grams: 100,
      is_active: true,
      popularity: 0,
      embedding: `[${embeddings[i].join(",")}]`,
    }));

    // ignore-duplicates: data is static, no need to overwrite on re-run.
    const CHUNK = 10;
    for (let c = 0; c < rows.length; c += CHUNK) {
      await upsertBatch("food_catalog", rows.slice(c, c + CHUNK), "source,ext_id", false);
    }
    total += foods.length;
    console.log(`  Page ${page} — fetched ${foods.length}, total so far: ${total}`);

    if (foods.length < USDA_PAGE_SIZE) {
      console.log("  Last page.");
      break;
    }

    await new Promise((r) => setTimeout(r, 250));
    page++;
  }

  console.log(`\n  ✅ ${label} done: ${total} items.`);
  return total;
}

async function main() {
  const start = Date.now();
  console.log("🚀 USDA Foundation + FNDDS import");

  const foundationCount = await importDataset(
    "Foundation Foods",
    "Foundation",
    "usda_foundation",
  );
  const fnddsCount = await importDataset(
    "Survey (FNDDS)",
    "Survey (FNDDS)",
    "usda_fndds",
  );

  const elapsed = Math.round((Date.now() - start) / 1000);
  console.log("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  console.log(`✅ Done in ${elapsed}s`);
  console.log(`   • Foundation Foods: ${foundationCount}`);
  console.log(`   • Survey FNDDS:     ${fnddsCount}`);
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
}

main().catch((e) => {
  console.error("❌ Import failed:", e);
  Deno.exit(1);
});
