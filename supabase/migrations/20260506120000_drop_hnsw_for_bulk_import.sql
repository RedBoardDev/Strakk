-- Temporarily drop HNSW index to allow fast bulk import of Branded Foods.
-- The index will be recreated after the import completes.
-- Without this, each INSERT triggers an O(log n) HNSW graph update that
-- causes PostgREST statement timeouts on 1536-dim vectors.

DROP INDEX IF EXISTS idx_food_catalog_embedding;
