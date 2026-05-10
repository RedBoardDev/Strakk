/**
 * run-all.ts — V3 meal analysis: complete data pipeline in one shot.
 *
 * Steps:
 *   1. Embed all CIQUAL/OFF rows missing an embedding (paginated).
 *   2. Import USDA SR Legacy from FDC API + generate embeddings.
 *   3. Import USDA Foundation Foods (higher quality nutritional data).
 *   4. Import USDA Survey FNDDS (composite dishes — pizza, stews, etc.).
 *   5. Print the HNSW index SQL (Supabase managed often blocks DDL via REST,
 *      so we instruct the user to paste it in the SQL editor).
 *
 * Idempotent at every step: re-runnable safely.
 *
 * Usage:
 *   deno run --allow-net --allow-env --env=.env run-all.ts
 */

import { embedBatched } from "./_lib/openai.ts";
import { rpc, select, updateById, upsertBatch } from "./_lib/supabase.ts";

// ---------------------------------------------------------------------------
// Config
// ---------------------------------------------------------------------------

const EMBED_BATCH = 100;
const CIQUAL_PAGE_SIZE = 1000; // PostgREST default cap
const USDA_PAGE_SIZE = 200;
const USDA_SAFETY_PAGE_LIMIT = 60;

// USDA legacy nutrient numbers as returned by /foods/list (AbridgedFoodNutrient.number).
// These are the 3-digit USDA legacy numbers, NOT the 4-digit FDC nutrientIds.
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

// ---------------------------------------------------------------------------
// Step 1 — Embed CIQUAL / OFF rows
// ---------------------------------------------------------------------------

interface FoodRow {
  id: number;
  name: string;
}

async function embedCiqualPage(rows: FoodRow[]): Promise<number> {
  if (rows.length === 0) return 0;
  const names = rows.map((r) => r.name);
  const embeddings = await embedBatched(
    names,
    EMBED_BATCH,
    (done, total) => console.log(`    Embedding ${done}/${total}...`),
  );

  console.log("    Updating food_catalog...");
  let updated = 0;
  for (let i = 0; i < rows.length; i++) {
    await updateById("food_catalog", rows[i].id, {
      embedding: `[${embeddings[i].join(",")}]`,
    });
    updated++;
    if (updated % 100 === 0) console.log(`    Updated ${updated}/${rows.length}`);
  }
  return updated;
}

async function embedAllCiqual(): Promise<number> {
  console.log("\n=== Step 1 — Embedding CIQUAL / OFF rows ===");
  let total = 0;
  let page = 1;

  while (true) {
    console.log(`\n  Page ${page} (up to ${CIQUAL_PAGE_SIZE} items)`);
    const rows = await select<FoodRow>(
      "food_catalog",
      `select=id,name&source=neq.usda&embedding=is.null&is_active=eq.true&order=id.asc&limit=${CIQUAL_PAGE_SIZE}`,
    );
    console.log(`  Found ${rows.length} items without embeddings.`);

    if (rows.length === 0) {
      console.log("  No more rows to embed.");
      break;
    }

    const updated = await embedCiqualPage(rows);
    total += updated;
    console.log(`  Page ${page} done — ${updated} items updated.`);
    page++;
  }

  console.log(`\n  ✅ Step 1 complete: ${total} CIQUAL/OFF items embedded.`);
  return total;
}

// ---------------------------------------------------------------------------
// Step 2 — Import USDA SR Legacy
// ---------------------------------------------------------------------------

interface FdcFood {
  fdcId: number;
  description: string;
  foodNutrients?: { nutrientId?: number; number?: string; amount?: number; value?: number }[];
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
    throw new Error(`Unexpected FDC response shape: ${JSON.stringify(json).slice(0, 200)}`);
  }
  throw new Error(`FDC API failed after retries on page ${page}`);
}

function nutrientValue(food: FdcFood, number: string): number | null {
  const found = food.foodNutrients?.find((n) => n.number === number);
  if (!found) return null;
  return (found.amount ?? found.value) ?? null;
}

function normalize(name: string): string {
  return name.toLowerCase().trim().replace(/\s+/g, " ");
}

async function importUsdaDataset(stepLabel: string, fdcDataType: string, source: string): Promise<number> {
  console.log(`\n=== ${stepLabel} ===`);
  if (!USDA_API_KEY) {
    console.warn("  ⚠️  USDA_API_KEY not set — skipping.");
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

    // Chunk upserts to avoid PostgREST statement timeout (large vector payloads).
    // Use ignore-duplicates: USDA data is static, no need to merge on re-runs.
    const CHUNK = 10;
    for (let c = 0; c < rows.length; c += CHUNK) {
      await upsertBatch("food_catalog", rows.slice(c, c + CHUNK), "source,ext_id", false);
    }
    total += foods.length;
    console.log(`  Page ${page} — fetched ${foods.length}, total: ${total}`);

    if (foods.length < USDA_PAGE_SIZE) {
      console.log("  Last page reached (fewer items than pageSize).");
      break;
    }

    await new Promise((r) => setTimeout(r, 250));
    page++;
  }

  console.log(`\n  ✅ ${stepLabel} complete: ${total} items processed.`);
  return total;
}

// ---------------------------------------------------------------------------
// Step 5 — HNSW index instructions
// ---------------------------------------------------------------------------

function printIndexInstructions(): void {
  const sql = `CREATE INDEX IF NOT EXISTS idx_food_catalog_embedding
  ON food_catalog
  USING hnsw (embedding extensions.vector_cosine_ops);`;

  console.log("\n=== Step 5 — HNSW index ===");
  console.log("");
  console.log("  Supabase managed projects don't allow arbitrary DDL via REST.");
  console.log("  Run this SQL once in the Supabase SQL editor:");
  console.log("");
  console.log("  https://supabase.com/dashboard → SQL editor");
  console.log("");
  console.log("  ──────────────────────────────────────────────────────");
  console.log(sql.split("\n").map((l) => "  " + l).join("\n"));
  console.log("  ──────────────────────────────────────────────────────");
  console.log("");
  console.log("  This index is REQUIRED for fast vector search at runtime.");
  console.log("  Without it, search_food_catalog_vector falls back to seq scan");
  console.log("  (still correct, but slower at scale).");
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main() {
  const start = Date.now();
  console.log("🚀 V3 meal analysis — full data pipeline");
  console.log("    1) Embed CIQUAL/OFF  →  2) SR Legacy  →  3) Foundation  →  4) FNDDS  →  5) Index SQL");

  const ciqualCount = await embedAllCiqual();
  const srLegacyCount = await importUsdaDataset(
    "Step 2 — Importing USDA SR Legacy",
    "SR Legacy",
    "usda",
  );
  const foundationCount = await importUsdaDataset(
    "Step 3 — Importing USDA Foundation Foods",
    "Foundation",
    "usda_foundation",
  );
  const fnddsCount = await importUsdaDataset(
    "Step 4 — Importing USDA Survey (FNDDS)",
    "Survey (FNDDS)",
    "usda_fndds",
  );
  printIndexInstructions();

  const elapsed = Math.round((Date.now() - start) / 1000);
  console.log("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  console.log(`✅ Pipeline finished in ${elapsed}s`);
  console.log(`   • CIQUAL/OFF embedded:       ${ciqualCount}`);
  console.log(`   • USDA SR Legacy imported:   ${srLegacyCount}`);
  console.log(`   • USDA Foundation imported:  ${foundationCount}`);
  console.log(`   • USDA FNDDS imported:       ${fnddsCount}`);
  console.log("   • Don't forget to run the HNSW index SQL above.");
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
}

main().catch((e) => {
  console.error("❌ Pipeline failed:", e);
  Deno.exit(1);
});
