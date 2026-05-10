-- ================================================================
-- food_catalog: add vector columns + USDA source support (V3)
-- ================================================================
-- Adds:
--   embedding  vector(1536)  — text-embedding-3-small, populated by import script
--   density    numeric       — g/mL, for volume→mass conversion (cups/ml units)
--   fdc_id     bigint        — USDA FoodData Central ID (null for non-USDA items)
--
-- Expands source constraint to include 'usda'.
-- Creates a dedicated ID sequence for USDA items (100M..999M range,
-- above CIQUAL <100K, below OFF >=1B — no collision).
--
-- Updates search_food_catalog to exclude USDA items: their English
-- names don't work with French FTS/trigram. USDA participates in
-- vector search only (search_food_catalog_vector).
--
-- IVFFlat index on embedding is NOT created here — it requires data
-- to be loaded first. Created by tools/import-embeddings/ after load.
-- ================================================================

----------------------------------------------------------------
-- 1. New columns
----------------------------------------------------------------
ALTER TABLE food_catalog
    ADD COLUMN IF NOT EXISTS embedding extensions.vector(1536),
    ADD COLUMN IF NOT EXISTS density   numeric CHECK (density IS NULL OR density > 0),
    ADD COLUMN IF NOT EXISTS fdc_id    bigint;

-- One row per USDA FDC item (null allowed for non-USDA)
ALTER TABLE food_catalog
    DROP CONSTRAINT IF EXISTS food_catalog_fdc_id_key;
ALTER TABLE food_catalog
    ADD CONSTRAINT food_catalog_fdc_id_key UNIQUE (fdc_id) DEFERRABLE INITIALLY DEFERRED;

----------------------------------------------------------------
-- 2. Expand source constraint → include 'usda'
----------------------------------------------------------------
ALTER TABLE food_catalog DROP CONSTRAINT IF EXISTS food_catalog_source_check;
ALTER TABLE food_catalog
    ADD CONSTRAINT food_catalog_source_check
    CHECK (source IN ('ciqual', 'off_fr', 'off_live', 'manual_admin', 'usda'));

----------------------------------------------------------------
-- 3. ID sequence for USDA rows (100_000_000 .. 999_999_999)
----------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS food_catalog_usda_id_seq
    START WITH 100000000
    INCREMENT BY 1
    MINVALUE 100000000
    MAXVALUE 999999999;

----------------------------------------------------------------
-- 4. Helper: allocate USDA IDs in one round-trip (used by import script)
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION nextvals_usda_seq(count int)
RETURNS bigint[]
LANGUAGE sql
VOLATILE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT array_agg(nextval('food_catalog_usda_id_seq')::bigint)
    FROM generate_series(1, GREATEST(count, 0));
$$;

REVOKE ALL ON FUNCTION nextvals_usda_seq(int) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nextvals_usda_seq(int) TO service_role;

----------------------------------------------------------------
-- 5. Update search_food_catalog — exclude USDA from text search
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION search_food_catalog(q text, lim int DEFAULT 20)
RETURNS TABLE (
    id                    bigint,
    source                text,
    name                  text,
    brand                 text,
    protein               double precision,
    calories              double precision,
    fat                   double precision,
    carbs                 double precision,
    default_portion_grams double precision,
    serving_label         text,
    nutriscore            char(1),
    nova_group            smallint,
    barcode               text,
    image_url             text,
    rank                  real
)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = public
AS $$
WITH normalized AS (
    SELECT
        lower(immutable_unaccent(trim(q))) AS nq,
        websearch_to_tsquery('french', immutable_unaccent(coalesce(q, ''))) AS tq
),
fts_hits AS (
    SELECT
        fc.id, fc.source, fc.name, fc.brand,
        fc.protein, fc.calories, fc.fat, fc.carbs,
        fc.default_portion_grams, fc.serving_label,
        fc.nutriscore, fc.nova_group, fc.barcode, fc.image_url,
        fc.popularity, fc.name_normalized, fc.brand_normalized,
        ts_rank_cd(fc.search_vector, n.tq) * 100.0 AS r
    FROM food_catalog fc, normalized n
    WHERE fc.is_active
      AND fc.source != 'usda'
      AND n.tq IS NOT NULL
      AND fc.search_vector @@ n.tq
    ORDER BY ts_rank_cd(fc.search_vector, n.tq) DESC
    LIMIT lim * 3
),
trgm_hits AS (
    SELECT
        fc.id, fc.source, fc.name, fc.brand,
        fc.protein, fc.calories, fc.fat, fc.carbs,
        fc.default_portion_grams, fc.serving_label,
        fc.nutriscore, fc.nova_group, fc.barcode, fc.image_url,
        fc.popularity, fc.name_normalized, fc.brand_normalized,
        GREATEST(
            similarity(fc.name_normalized, n.nq),
            coalesce(similarity(fc.brand_normalized, n.nq), 0)
        ) * 50.0 AS r
    FROM food_catalog fc, normalized n
    WHERE fc.is_active
      AND fc.source != 'usda'
      AND n.nq <> ''
      AND (fc.name_normalized % n.nq OR fc.brand_normalized % n.nq)
    ORDER BY GREATEST(
        similarity(fc.name_normalized, n.nq),
        coalesce(similarity(fc.brand_normalized, n.nq), 0)
    ) DESC
    LIMIT lim * 3
),
combined AS (
    SELECT * FROM fts_hits
    UNION ALL
    SELECT * FROM trgm_hits
),
deduped AS (
    SELECT DISTINCT ON (name_normalized, coalesce(brand_normalized, ''))
        id, source, name, brand,
        protein, calories, fat, carbs,
        default_portion_grams, serving_label,
        nutriscore, nova_group, barcode, image_url,
        (r + (popularity::real / 1000.0) * 5.0)::real AS final_rank
    FROM combined
    ORDER BY name_normalized, coalesce(brand_normalized, ''), r DESC
)
SELECT
    id, source, name, brand,
    protein, calories, fat, carbs,
    default_portion_grams, serving_label,
    nutriscore, nova_group, barcode, image_url,
    final_rank AS rank
FROM deduped
ORDER BY final_rank DESC
LIMIT GREATEST(lim, 1);
$$;

REVOKE ALL ON FUNCTION search_food_catalog(text, int) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION search_food_catalog(text, int) TO authenticated;
