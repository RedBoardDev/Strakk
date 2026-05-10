/**
 * run-branded.ts — Import USDA Branded Foods (~400K items).
 * Long-running (2-3h). Restart-safe via ignore-duplicates upsert.
 * HNSW index should be DROPPED before running this.
 *
 * Usage:
 *   deno run --allow-net --allow-env --env=.env run-branded.ts [startPage]
 */

import { embedBatched } from "./_lib/openai.ts";
import { upsertBatch } from "./_lib/supabase.ts";

const EMBED_BATCH = 100;
const PAGE_SIZE = 200;
const MAX_PAGES = 2500;
const CHUNK = 25; // larger chunks OK without HNSW index

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

async function fetchPage(page: number): Promise<FdcFood[]> {
  for (let attempt = 0; attempt < 4; attempt++) {
    const url =
      `${FDC_BASE}/foods/list?dataType=Branded&pageSize=${PAGE_SIZE}&pageNumber=${page}&api_key=${USDA_API_KEY}`;
    const res = await fetch(url);
    if (res.status === 429) {
      const delay = Math.pow(2, attempt) * 2000;
      console.warn(`  Rate limit — retry ${delay}ms`);
      await new Promise((r) => setTimeout(r, delay));
      continue;
    }
    if (res.status === 400) return [];
    if (!res.ok) throw new Error(`FDC ${res.status}: ${(await res.text()).slice(0, 200)}`);
    const json = await res.json();
    return (Array.isArray(json) ? json : json.foods ?? []) as FdcFood[];
  }
  throw new Error(`FDC failed on page ${page}`);
}

function nv(food: FdcFood, num: string): number | null {
  const f = food.foodNutrients?.find((n) => n.number === num);
  return f ? ((f.amount ?? f.value) ?? null) : null;
}

function norm(s: string): string {
  return s.toLowerCase().trim().replace(/\s+/g, " ");
}

async function main() {
  const startPage = parseInt(Deno.args[0] ?? "1", 10);
  console.log(`=== Branded Foods import (starting page ${startPage}) ===`);
  if (!USDA_API_KEY) { console.error("USDA_API_KEY not set"); Deno.exit(1); }

  let total = 0;
  let page = startPage;

  while (page <= MAX_PAGES) {
    let foods: FdcFood[];
    try {
      foods = await fetchPage(page);
    } catch (err) {
      console.error(`  Page ${page} error:`, err);
      console.log("  Retrying in 5s...");
      await new Promise((r) => setTimeout(r, 5000));
      try { foods = await fetchPage(page); } catch { break; }
    }

    if (foods.length === 0) {
      console.log(`  Page ${page} empty — done.`);
      break;
    }

    const names = foods.map((f) => f.description);
    let embeddings: number[][];
    try {
      embeddings = await embedBatched(names, EMBED_BATCH);
    } catch (err) {
      console.error(`  Embedding failed page ${page}:`, err);
      console.log("  Retrying embeddings in 5s...");
      await new Promise((r) => setTimeout(r, 5000));
      try { embeddings = await embedBatched(names, EMBED_BATCH); } catch { break; }
    }

    const rows = foods.map((food, i) => ({
      name: food.description,
      name_normalized: norm(food.description),
      source: "usda_branded",
      ext_id: String(food.fdcId),
      fdc_id: food.fdcId,
      protein: nv(food, NUTRIENT.protein) ?? 0,
      calories: nv(food, NUTRIENT.kcal) ?? 0,
      fat: nv(food, NUTRIENT.fat),
      carbs: nv(food, NUTRIENT.carbs),
      fiber_100g: nv(food, NUTRIENT.fiber),
      sugar_100g: nv(food, NUTRIENT.sugar),
      default_portion_grams: 100,
      is_active: true,
      popularity: 0,
      embedding: `[${embeddings![i].join(",")}]`,
    }));

    for (let c = 0; c < rows.length; c += CHUNK) {
      for (let attempt = 0; attempt < 3; attempt++) {
        try {
          await upsertBatch("food_catalog", rows.slice(c, c + CHUNK), "source,ext_id", false);
          break;
        } catch (err) {
          if (attempt < 2) {
            console.warn(`  Upsert retry (chunk ${c}, attempt ${attempt + 1})`);
            await new Promise((r) => setTimeout(r, 2000));
          } else {
            throw err;
          }
        }
      }
    }

    total += foods.length;
    if (page % 10 === 0) {
      console.log(`  Page ${page} — total: ${total} (${new Date().toISOString().slice(11, 19)})`);
    }

    if (foods.length < PAGE_SIZE) {
      console.log("  Last page.");
      break;
    }

    await new Promise((r) => setTimeout(r, 200));
    page++;
  }

  console.log(`\n=== Done: ${total} branded items imported ===`);
  console.log("IMPORTANT: Recreate the HNSW index now!");
}

main().catch((e) => {
  console.error("FATAL:", e);
  Deno.exit(1);
});
