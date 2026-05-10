/**
 * Quick diagnostic: embed a food name and test vector search.
 * Usage: deno run --allow-net --allow-env --env=.env test-search.ts
 */

import { embedBatched } from "./_lib/openai.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL");
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

const testNames = [
  "chicken breast, grilled",
  "white rice, cooked",
  "olive oil",
  "pizza",
  "scrambled eggs",
];

async function main() {
  console.log("Embedding test names...");
  const embeddings = await embedBatched(testNames, 5);
  console.log(`Got ${embeddings.length} embeddings, dim=${embeddings[0].length}`);

  for (let i = 0; i < testNames.length; i++) {
    const name = testNames[i];
    const embedding = embeddings[i];

    console.log(`\n--- "${name}" ---`);

    const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/search_food_catalog_vector`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "apikey": SERVICE_ROLE_KEY!,
        "Authorization": `Bearer ${SERVICE_ROLE_KEY}`,
      },
      body: JSON.stringify({
        query_embedding: `[${embedding.join(",")}]`,
        match_threshold: 0.4,
        match_count: 5,
        require_density: false,
      }),
    });

    if (!res.ok) {
      const txt = await res.text();
      console.error(`  RPC ERROR ${res.status}: ${txt.slice(0, 300)}`);
      continue;
    }

    const matches = await res.json();
    if (matches.length === 0) {
      console.log("  NO MATCHES (threshold 0.4)");
    } else {
      for (const m of matches) {
        console.log(
          `  [${m.source}] ${m.name} — sim=${m.similarity?.toFixed(3)} kcal=${m.kcal} prot=${m.protein} fat=${m.fat} carbs=${m.carbs}`,
        );
      }
    }
  }

  // Also check total items with embeddings
  const countRes = await fetch(
    `${SUPABASE_URL}/rest/v1/food_catalog?select=source&embedding=not.is.null&is_active=eq.true&limit=1`,
    {
      headers: {
        "apikey": SERVICE_ROLE_KEY!,
        "Authorization": `Bearer ${SERVICE_ROLE_KEY}`,
        "Prefer": "count=exact",
      },
    },
  );
  const totalCount = countRes.headers.get("content-range");
  console.log(`\nTotal active items with embeddings: ${totalCount}`);
}

main().catch((e) => {
  console.error("FAILED:", e);
  Deno.exit(1);
});
