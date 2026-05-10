-- HNSW index for fast vector similarity search on food_catalog.
-- Required for search_food_catalog_vector to avoid seq scan timeouts
-- on 16K+ rows with 1536-dim vectors.
-- m=16, ef_construction=64 are good defaults for this scale.
-- pgvector lives in the extensions schema on Supabase managed projects.

SET search_path = public, extensions;

CREATE INDEX IF NOT EXISTS idx_food_catalog_embedding
ON food_catalog
USING hnsw (embedding extensions.vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
