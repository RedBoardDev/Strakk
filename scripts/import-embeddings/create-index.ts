/**
 * Create HNSW index on food_catalog.embedding via pg_net/Supabase SQL.
 * Usage: deno run --allow-net --allow-env --env=.env create-index.ts
 */

const SUPABASE_URL = Deno.env.get("SUPABASE_URL");
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

async function main() {
  console.log("Creating HNSW index on food_catalog.embedding...");
  console.log("This may take 30-60 seconds on ~17K rows.");

  const sql = `
    CREATE INDEX IF NOT EXISTS idx_food_catalog_embedding
    ON food_catalog
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
  `;

  const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/pg_execute`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "apikey": SERVICE_ROLE_KEY!,
      "Authorization": `Bearer ${SERVICE_ROLE_KEY}`,
    },
    body: JSON.stringify({ query: sql }),
  });

  if (!res.ok) {
    const text = await res.text();
    console.error(`Failed ${res.status}: ${text}`);
    console.log("\nFallback: run this SQL manually in Supabase SQL editor:");
    console.log(sql);
  } else {
    console.log("HNSW index created successfully!");
  }
}

main().catch(console.error);
