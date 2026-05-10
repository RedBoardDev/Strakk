-- Rebuild HNSW index with more maintenance_work_mem to avoid disk spills.
-- The initial build only fit 4881 tuples in memory (out of ~17K).
-- Setting 512MB should comfortably hold all vectors during build.

SET maintenance_work_mem = '512MB';

DROP INDEX IF EXISTS idx_food_catalog_embedding;

CREATE INDEX idx_food_catalog_embedding
ON food_catalog
USING hnsw (embedding extensions.vector_cosine_ops)
WITH (m = 16, ef_construction = 200);

RESET maintenance_work_mem;
