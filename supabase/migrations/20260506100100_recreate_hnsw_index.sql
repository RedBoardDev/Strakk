-- Drop and recreate HNSW index with correct operator class.
-- The previous index was either IVFFlat or created with wrong ops.

DROP INDEX IF EXISTS idx_food_catalog_embedding;

CREATE INDEX idx_food_catalog_embedding
ON food_catalog
USING hnsw (embedding extensions.vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
