# Import Embeddings — V3 Meal Analysis

One-shot script that prepares the `food_catalog` for V3 semantic search.
Run **once** after deploying the V3 migrations.

## Prerequisites

- [Deno](https://deno.land/) (`brew install deno`)
- V3 migrations deployed (`supabase db push`)
- API keys ready

## Setup

Create `.env` in this folder (already gitignored):

```bash
SUPABASE_URL=https://xxxxxxxxxxxx.supabase.co
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
OPENAI_API_KEY=sk-...
USDA_API_KEY=...   # Free at https://fdc.nal.usda.gov/api-key-signup
```

## Run

```bash
deno run --allow-net --allow-env --env=.env run-all.ts
```

The script does everything in sequence:
1. **Embed CIQUAL/OFF** — paginates through all rows where `embedding IS NULL`
2. **Import USDA SR Legacy** — fetches ~7800 items from FDC API + embeds them
3. **Print IVFFlat SQL** — copy/paste into Supabase SQL editor

Idempotent at every step. Re-running:
- Skips already-embedded rows (CIQUAL)
- Skips already-imported rows via `ON CONFLICT (source, ext_id) DO NOTHING` (USDA)

Total cost: ~$0.23 (one-shot OpenAI embeddings).
Total time: ~7-10 minutes (first run).

## Final step

After the script finishes, paste the printed SQL into:
**Supabase Dashboard → SQL editor**

```sql
CREATE INDEX IF NOT EXISTS idx_food_catalog_embedding
  ON food_catalog
  USING hnsw (embedding extensions.vector_cosine_ops);
```

HNSW index — required for fast vector search. Construction is incremental
(no memory-hungry training phase like IVFFlat). Better recall and faster
queries than IVFFlat for our scale (~11K vectors).

## Verify

```sql
SELECT source, COUNT(*) AS total, COUNT(embedding) AS with_embedding
FROM food_catalog
GROUP BY source;
```

Expected:
- `ciqual` ≈ 3 484 / 3 484
- `off_fr` / `off_live` (varies)
- `usda` ≈ 7 800 / 7 800
