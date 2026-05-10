/**
 * Check what vector indexes exist on food_catalog.
 * Usage: deno run --allow-net --allow-env --env=.env check-index.ts
 */

const SUPABASE_URL = Deno.env.get("SUPABASE_URL");
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

async function query(sql: string): Promise<unknown> {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/exec_sql`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "apikey": SERVICE_ROLE_KEY!,
      "Authorization": `Bearer ${SERVICE_ROLE_KEY}`,
    },
    body: JSON.stringify({ sql }),
  });
  if (!res.ok) return `ERROR ${res.status}: ${await res.text()}`;
  return res.json();
}

async function main() {
  // Check indexes on food_catalog using pg_indexes
  const idxRes = await fetch(
    `${SUPABASE_URL}/rest/v1/rpc/exec_sql`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "apikey": SERVICE_ROLE_KEY!,
        "Authorization": `Bearer ${SERVICE_ROLE_KEY}`,
      },
      body: JSON.stringify({
        sql: "SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'food_catalog' AND indexdef LIKE '%embedding%'",
      }),
    },
  );
  if (!idxRes.ok) {
    console.log("exec_sql not available, trying direct REST query...");

    // Fallback: just test if vector search is fast now
    const { embedBatched } = await import("./_lib/openai.ts");
    console.log("Embedding 'chicken breast, grilled'...");
    const [[emb]] = [await embedBatched(["chicken breast, grilled"], 1)];

    const t0 = Date.now();
    const searchRes = await fetch(`${SUPABASE_URL}/rest/v1/rpc/search_food_catalog_vector`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "apikey": SERVICE_ROLE_KEY!,
        "Authorization": `Bearer ${SERVICE_ROLE_KEY}`,
      },
      body: JSON.stringify({
        query_embedding: `[${emb.join(",")}]`,
        match_threshold: 0.4,
        match_count: 5,
        require_density: false,
      }),
    });
    const elapsed = Date.now() - t0;

    if (!searchRes.ok) {
      const txt = await searchRes.text();
      console.error(`Vector search ERROR ${searchRes.status} (${elapsed}ms): ${txt.slice(0, 300)}`);
    } else {
      const matches = await searchRes.json();
      console.log(`Vector search returned ${matches.length} matches in ${elapsed}ms`);
      for (const m of matches) {
        console.log(`  [${m.source}] ${m.name} — sim=${m.similarity?.toFixed(3)} kcal=${m.kcal}`);
      }
    }
  } else {
    const data = await idxRes.json();
    console.log("Index info:", JSON.stringify(data, null, 2));
  }
}

main().catch(console.error);
