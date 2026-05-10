#!/usr/bin/env -S deno run --allow-net --allow-env --allow-read --allow-write --allow-run

/**
 * USDA FoodData Central → Qdrant import pipeline.
 *
 * Reproduces the macroscanner / fdcetl pipeline (jakesteelman, MIT) in Deno:
 *   1. Download the FDC CSV bundle (single zip, all data — no API key needed).
 *   2. food.csv      → keep SR Legacy + Foundation + Survey FNDDS rows.
 *   3. food_nutrient → pivot to one row per fdc_id with macros + extras.
 *   4. food_portion  → derive density (g/mL) from volume portions; median per food.
 *   5. Drop foods with no kcal/protein/fat/carbs.
 *   6. Embed descriptions with OpenAI text-embedding-3-small.
 *   7. Upsert into Qdrant.
 *
 * Required env:
 *   OPENAI_API_KEY  — embeddings
 *   QDRANT_URL      — e.g. http://qdrant:6333
 *   QDRANT_API_KEY  — optional
 *
 * The CSV bundle is downloaded once and cached in /app/import/data/.
 * Re-running the script will reuse the cached bundle (delete the directory
 * to force a fresh download).
 */

import { CsvParseStream } from "@std/csv";
import { embedTexts } from "./openai.ts";

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

const QDRANT_URL = Deno.env.get("QDRANT_URL") ?? "http://localhost:6333";
const QDRANT_API_KEY = Deno.env.get("QDRANT_API_KEY") || undefined;

const COLLECTION = "food_catalog";
const VECTOR_SIZE = 1536;
const UPSERT_BATCH = 25;
const EMBED_BATCH = 50;

const DATA_DIR = "import/data";
const FDC_DATASET_DATE = "2024-10-31"; // FDC publishes a refresh ~twice a year
const FDC_ZIP_URL =
  `https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_csv_${FDC_DATASET_DATE}.zip`;
const FDC_DIR = `${DATA_DIR}/FoodData_Central_csv_${FDC_DATASET_DATE}`;

// FDC nutrient IDs we extract — full set used by macroscanner / fdcetl.
const NUTRIENT_IDS = {
  kcal: 1008,
  protein: 1003,
  fat: 1004,
  carbs: 1005,
  fiber: 1079,
  sugar: 2000,
  sugar_added: 1235,
  sodium: 1093,
  cholesterol: 1253,
  fat_sat: 1258,
  fat_trans: 1257,
  fat_mono: 1292,
  fat_poly: 1293,
  alcohol: 1018,
  caffeine: 1057,
} as const;
type NutrientKey = keyof typeof NUTRIENT_IDS;

const NUTRIENT_ID_SET = new Set<number>(Object.values(NUTRIENT_IDS));
const NUTRIENT_ID_TO_KEY = new Map<number, NutrientKey>(
  (Object.entries(NUTRIENT_IDS) as [NutrientKey, number][])
    .map(([k, v]) => [v, k]),
);

// data_type → user-facing source label (matches FoodCatalogSource on the client)
const DATA_TYPE_TO_SOURCE: Record<string, string> = {
  sr_legacy_food: "usda",
  foundation_food: "usda_foundation",
  survey_fndds_food: "usda_fndds",
};
const ALLOWED_DATA_TYPES = new Set(Object.keys(DATA_TYPE_TO_SOURCE));

// Volume → mL conversion (Pint values used by fdcetl).
const VOLUME_TO_ML: Record<string, number> = {
  milliliter: 1,
  liter: 1000,
  fluid_ounce: 29.5735,
  cup: 236.588,
  pint: 473.176,
  quart: 946.353,
  gallon: 3785.41,
  teaspoon: 4.92892,
  tablespoon: 14.7868,
};

// Free-form unit text → canonical name. Anything unmapped is dropped.
const UNIT_ALIASES: Record<string, string> = {
  ml: "milliliter", milliliter: "milliliter", milliliters: "milliliter",
  l: "liter", liter: "liter", liters: "liter",
  cup: "cup", cups: "cup",
  tsp: "teaspoon", teaspoon: "teaspoon", teaspoons: "teaspoon",
  tbsp: "tablespoon", tablespoon: "tablespoon", tablespoons: "tablespoon",
  fluid_ounce: "fluid_ounce",
  oz: "ounce", ounce: "ounce", ounces: "ounce",
  pt: "pint", pint: "pint", pints: "pint",
  qt: "quart", quart: "quart", quarts: "quart",
  gal: "gallon", gallon: "gallon", gallons: "gallon",
};

const VOLUME_UNITS = new Set(Object.keys(VOLUME_TO_ML));

// Plausible density bounds (g/mL). Helps reject parsing errors like
// "1 lb cooked" wrongly mapped to a volume unit.
const MIN_DENSITY = 0.1;
const MAX_DENSITY = 5.0;

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

interface FoodMeta {
  fdcId: number;
  description: string;
  dataType: string;
}

type NutrientValues = { [K in NutrientKey]: number | null };

interface FoodItem extends NutrientValues {
  id: number;
  source: string;
  name: string;
  density: number | null;
  default_portion_grams: number;
}

interface QdrantPoint {
  id: number;
  vector: number[];
  payload: Record<string, unknown>;
}

// ---------------------------------------------------------------------------
// Bundle download + extract
// ---------------------------------------------------------------------------

async function pathExists(p: string): Promise<boolean> {
  try {
    await Deno.stat(p);
    return true;
  } catch {
    return false;
  }
}

async function ensureFdcBundle(): Promise<void> {
  await Deno.mkdir(DATA_DIR, { recursive: true });

  if (await pathExists(`${FDC_DIR}/food.csv`)) {
    console.log(`[fdc] Bundle already extracted at ${FDC_DIR}`);
    return;
  }

  const zipPath = `${DATA_DIR}/FoodData_Central_csv_${FDC_DATASET_DATE}.zip`;

  if (!(await pathExists(zipPath))) {
    console.log(`[fdc] Downloading ${FDC_ZIP_URL}`);
    await runOrThrow("curl", ["-fL", "--retry", "3", "-o", zipPath, FDC_ZIP_URL]);
  } else {
    console.log(`[fdc] Using cached zip at ${zipPath}`);
  }

  console.log(`[fdc] Extracting ${zipPath} → ${DATA_DIR}/`);
  await runOrThrow("unzip", ["-q", "-o", zipPath, "-d", DATA_DIR]);
}

async function runOrThrow(cmd: string, args: string[]): Promise<void> {
  const child = new Deno.Command(cmd, { args, stdout: "inherit", stderr: "inherit" });
  const { code } = await child.output();
  if (code !== 0) throw new Error(`${cmd} ${args.join(" ")} → exit ${code}`);
}

// ---------------------------------------------------------------------------
// CSV streaming (uses @std/csv — handles quoted commas and embedded newlines)
// ---------------------------------------------------------------------------

async function* streamCsv(filePath: string): AsyncGenerator<Record<string, string>> {
  const file = await Deno.open(filePath, { read: true });
  const stream = file.readable
    .pipeThrough(new TextDecoderStream())
    .pipeThrough(new CsvParseStream({ skipFirstRow: true }));
  for await (const row of stream) {
    yield row as Record<string, string>;
  }
}

// ---------------------------------------------------------------------------
// Step 1 — load food.csv
// ---------------------------------------------------------------------------

async function loadFoods(): Promise<Map<number, FoodMeta>> {
  console.log("[step] Reading food.csv");
  const map = new Map<number, FoodMeta>();
  let total = 0;
  for await (const row of streamCsv(`${FDC_DIR}/food.csv`)) {
    total++;
    const dataType = row.data_type;
    if (!ALLOWED_DATA_TYPES.has(dataType)) continue;
    const fdcId = Number(row.fdc_id);
    const description = row.description?.trim() ?? "";
    if (!Number.isFinite(fdcId) || description.length === 0) continue;
    map.set(fdcId, { fdcId, description, dataType });
  }
  console.log(`[step] food.csv: kept ${map.size}/${total} rows`);
  return map;
}

// ---------------------------------------------------------------------------
// Step 2 — pivot food_nutrient.csv (the big one, ~700 MB uncompressed)
// ---------------------------------------------------------------------------

async function loadNutrients(
  fdcIds: Set<number>,
): Promise<Map<number, NutrientValues>> {
  console.log("[step] Streaming food_nutrient.csv");
  const map = new Map<number, NutrientValues>();
  let scanned = 0;
  for await (const row of streamCsv(`${FDC_DIR}/food_nutrient.csv`)) {
    scanned++;
    if (scanned % 5_000_000 === 0) console.log(`  scanned ${scanned} nutrient rows`);
    const fdcId = Number(row.fdc_id);
    if (!fdcIds.has(fdcId)) continue;
    const nutrientId = Number(row.nutrient_id);
    if (!NUTRIENT_ID_SET.has(nutrientId)) continue;
    const amount = Number(row.amount);
    if (!Number.isFinite(amount)) continue;

    let entry = map.get(fdcId);
    if (!entry) {
      entry = emptyNutrients();
      map.set(fdcId, entry);
    }
    const key = NUTRIENT_ID_TO_KEY.get(nutrientId)!;
    entry[key] = amount;
  }
  console.log(`[step] food_nutrient.csv: ${map.size} foods enriched (${scanned} rows scanned)`);
  return map;
}

function emptyNutrients(): NutrientValues {
  return { kcal: null, protein: null, fat: null, carbs: null, fiber: null, sugar: null, sodium: null };
}

// ---------------------------------------------------------------------------
// Step 3 — load measure_unit.csv (small)
// ---------------------------------------------------------------------------

async function loadMeasureUnits(): Promise<Map<number, string>> {
  console.log("[step] Reading measure_unit.csv");
  const map = new Map<number, string>();
  for await (const row of streamCsv(`${FDC_DIR}/measure_unit.csv`)) {
    const id = Number(row.id);
    const name = row.name?.trim().toLowerCase();
    if (Number.isFinite(id) && name) map.set(id, name);
  }
  return map;
}

// ---------------------------------------------------------------------------
// Step 4 — derive densities from food_portion.csv
//
// Each portion has gram_weight + a free-form text describing the portion
// (combining `amount`, `modifier`, `measure_unit.name`, `portion_description`).
// We extract the volume amount + unit, compute grams / volume_mL, then take
// the median across all volume portions for a given food.
// ---------------------------------------------------------------------------

const AMOUNT_UNIT_REGEX = /(\d*\.?\d+)\s*\(?\s*([a-zA-Z][\w\s\/]*?)\s*\)?\b/;

function extractAmountAndUnit(text: string): { amount: number; unit: string } | null {
  const normalized = text.toLowerCase()
    .replace(/fluid\s+ounces?/g, "fluid_ounce")
    .replace(/\bfl\.?\s*oz\b/g, "fluid_ounce");
  const match = normalized.match(AMOUNT_UNIT_REGEX);
  if (!match) return null;
  const amount = Number(match[1]);
  if (!Number.isFinite(amount) || amount <= 0) return null;
  const firstToken = match[2].trim().split(/[\s,/]+/)[0];
  if (!firstToken) return null;
  return { amount, unit: firstToken };
}

function densityFromPortion(
  row: Record<string, string>,
  measureUnits: Map<number, string>,
): number | null {
  const gramWeight = Number(row.gram_weight);
  if (!Number.isFinite(gramWeight) || gramWeight <= 0) return null;

  const amount = row.amount?.trim();
  const modifier = row.modifier?.trim();
  const portionDescription = row.portion_description?.trim();
  const measureUnitId = Number(row.measure_unit_id);
  const unitName = measureUnits.get(measureUnitId);

  // Build the same composite text macroscanner uses, in priority order.
  let text: string | null = null;
  if (amount && (modifier || unitName)) {
    const u = modifier && modifier !== "undetermined" ? modifier : unitName;
    if (u) text = `${amount} ${u}`;
  } else if (portionDescription) {
    text = portionDescription;
  }
  if (!text) return null;

  const parsed = extractAmountAndUnit(text);
  if (!parsed) return null;

  const canonical = UNIT_ALIASES[parsed.unit];
  if (!canonical || !VOLUME_UNITS.has(canonical)) return null;

  const volumeMl = parsed.amount * VOLUME_TO_ML[canonical];
  if (volumeMl <= 0) return null;

  const density = gramWeight / volumeMl;
  if (!Number.isFinite(density) || density < MIN_DENSITY || density > MAX_DENSITY) {
    return null;
  }
  return density;
}

async function loadDensities(
  fdcIds: Set<number>,
  measureUnits: Map<number, string>,
): Promise<Map<number, number>> {
  console.log("[step] Computing densities from food_portion.csv");
  const samples = new Map<number, number[]>();

  for await (const row of streamCsv(`${FDC_DIR}/food_portion.csv`)) {
    const fdcId = Number(row.fdc_id);
    if (!fdcIds.has(fdcId)) continue;

    const density = densityFromPortion(row, measureUnits);
    if (density === null) continue;

    const arr = samples.get(fdcId);
    if (arr) arr.push(density);
    else samples.set(fdcId, [density]);
  }

  const out = new Map<number, number>();
  for (const [fdcId, arr] of samples) {
    arr.sort((a, b) => a - b);
    const mid = Math.floor(arr.length / 2);
    const median = arr.length % 2 === 0 ? (arr[mid - 1] + arr[mid]) / 2 : arr[mid];
    out.set(fdcId, Math.round(median * 1000) / 1000);
  }
  console.log(`[step] food_portion.csv: ${out.size} foods with density`);
  return out;
}

// ---------------------------------------------------------------------------
// Step 5 — combine and filter
// ---------------------------------------------------------------------------

function buildFoodItems(
  foods: Map<number, FoodMeta>,
  nutrients: Map<number, NutrientValues>,
  densities: Map<number, number>,
): FoodItem[] {
  const items: FoodItem[] = [];
  let droppedNoMacros = 0;
  for (const [fdcId, meta] of foods) {
    const n = nutrients.get(fdcId) ?? emptyNutrients();
    if (n.kcal === null && n.protein === null && n.fat === null && n.carbs === null) {
      droppedNoMacros++;
      continue;
    }
    items.push({
      id: fdcId,
      source: DATA_TYPE_TO_SOURCE[meta.dataType] ?? "usda",
      name: meta.description,
      kcal: n.kcal,
      protein: n.protein,
      fat: n.fat,
      carbs: n.carbs,
      fiber: n.fiber,
      sugar: n.sugar,
      sodium: n.sodium,
      density: densities.get(fdcId) ?? null,
      default_portion_grams: 100,
    });
  }
  console.log(
    `[step] Built ${items.length} food items (dropped ${droppedNoMacros} for missing macros)`,
  );
  return items;
}

// ---------------------------------------------------------------------------
// Step 6 — Qdrant
// ---------------------------------------------------------------------------

function qdrantHeaders(): Record<string, string> {
  const h: Record<string, string> = { "Content-Type": "application/json" };
  if (QDRANT_API_KEY) h["api-key"] = QDRANT_API_KEY;
  return h;
}

async function ensureCollection(): Promise<void> {
  const res = await fetch(`${QDRANT_URL}/collections/${COLLECTION}`, { headers: qdrantHeaders() });
  if (res.ok) {
    console.log(`[qdrant] Collection "${COLLECTION}" already exists`);
    return;
  }
  const createRes = await fetch(`${QDRANT_URL}/collections/${COLLECTION}`, {
    method: "PUT",
    headers: qdrantHeaders(),
    body: JSON.stringify({
      vectors: { size: VECTOR_SIZE, distance: "Cosine" },
      optimizers_config: { indexing_threshold: 20000 },
      hnsw_config: { m: 16, ef_construct: 200 },
    }),
  });
  if (!createRes.ok) {
    throw new Error(`Failed to create collection: ${await createRes.text()}`);
  }
  console.log(`[qdrant] Created collection "${COLLECTION}"`);
}

async function upsertPoints(points: QdrantPoint[]): Promise<void> {
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const res = await fetch(
        `${QDRANT_URL}/collections/${COLLECTION}/points?wait=true`,
        {
          method: "PUT",
          headers: qdrantHeaders(),
          body: JSON.stringify({ points }),
        },
      );
      if (res.ok) return;
      throw new Error(`Qdrant upsert ${res.status}: ${(await res.text()).slice(0, 300)}`);
    } catch (err) {
      if (attempt === 3) throw err;
      console.warn(`  [qdrant] upsert attempt ${attempt}/3 failed, retrying in 2s`);
      await new Promise((r) => setTimeout(r, 2000));
    }
  }
}

async function embedAndUpsert(items: FoodItem[]): Promise<void> {
  console.log(`[step] Embedding + upserting ${items.length} items`);
  for (let i = 0; i < items.length; i += EMBED_BATCH) {
    const batch = items.slice(i, i + EMBED_BATCH);
    let vectors: number[][];
    try {
      vectors = await embedTexts(batch.map((x) => x.name));
    } catch (err) {
      console.error(
        `  [embed] failed at offset ${i}: ${err instanceof Error ? err.message : err} — retrying in 10s`,
      );
      await new Promise((r) => setTimeout(r, 10000));
      try {
        vectors = await embedTexts(batch.map((x) => x.name));
      } catch {
        console.error("  [embed] retry failed, skipping batch");
        continue;
      }
    }

    const points: QdrantPoint[] = batch.map((item, j) => ({
      id: item.id,
      vector: vectors[j],
      payload: {
        source: item.source,
        name: item.name,
        kcal: item.kcal,
        protein: item.protein,
        fat: item.fat,
        carbs: item.carbs,
        fiber: item.fiber,
        sugar: item.sugar,
        sodium: item.sodium,
        density: item.density,
        default_portion_grams: item.default_portion_grams,
      },
    }));

    for (let u = 0; u < points.length; u += UPSERT_BATCH) {
      await upsertPoints(points.slice(u, u + UPSERT_BATCH));
    }

    const progress = Math.min(i + EMBED_BATCH, items.length);
    if (progress % 1000 === 0 || progress === items.length) {
      console.log(`  [embed] ${progress}/${items.length} (${((progress / items.length) * 100).toFixed(1)}%)`);
    }
  }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

async function main(): Promise<void> {
  console.log("=== Strakk Food Catalog Import (FDC bundle) ===");
  console.log(`Qdrant URL: ${QDRANT_URL}`);
  console.log(`Dataset:    ${FDC_DATASET_DATE}\n`);

  await ensureFdcBundle();
  await ensureCollection();

  const foods = await loadFoods();
  const fdcIdSet = new Set(foods.keys());
  const nutrients = await loadNutrients(fdcIdSet);
  const measureUnits = await loadMeasureUnits();
  const densities = await loadDensities(fdcIdSet, measureUnits);
  const items = buildFoodItems(foods, nutrients, densities);

  if (items.length === 0) {
    console.log("\nNothing to import.");
    return;
  }

  await embedAndUpsert(items);
  console.log(`\n=== Import complete: ${items.length} items ===`);
}

await main();
