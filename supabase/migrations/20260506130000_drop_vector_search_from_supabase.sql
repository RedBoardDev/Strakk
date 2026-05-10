-- Move vector search to Qdrant on VPS.
-- Drop the embedding column and vector search function from Supabase
-- to free up storage on the free tier.
-- CIQUAL/OFF items stay for text search (FTS/trigram).
-- USDA items are removed from Supabase (served by Qdrant only).

-- 1. Drop vector search function (no longer needed)
DROP FUNCTION IF EXISTS search_food_catalog_vector(extensions.vector, float, int, boolean);

-- 2. Drop HNSW index (if it was recreated)
DROP INDEX IF EXISTS idx_food_catalog_embedding;

-- 3. Drop embedding column (biggest space saver)
ALTER TABLE food_catalog DROP COLUMN IF EXISTS embedding;

-- 4. Drop USDA-specific helpers
DROP FUNCTION IF EXISTS nextvals_usda_seq(int);
DROP SEQUENCE IF EXISTS food_catalog_usda_id_seq;

-- 5. Remove USDA items from Supabase (they live in Qdrant now)
DELETE FROM food_catalog WHERE source IN ('usda', 'usda_foundation', 'usda_fndds', 'usda_branded');
