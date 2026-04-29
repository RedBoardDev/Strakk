-- ================================================================
-- Extend food_catalog: brands, FTS, fuzzy search RPC
-- Plan: docs/plans/food-search-v2 (CIQUAL + OFF FR hybrid)
-- ================================================================
--
-- Adds:
--   * brand / barcode / nutriscore / nova / image_url / serving_label
--   * is_active (preserves history when products disappear from sources)
--   * popularity (used for ranking)
--   * search_vector (generated, GIN-indexed) for multi-word FTS
--   * unaccent extension for accent-insensitive search
--   * RPC search_food_catalog(q, lim) — FTS + trigram + popularity, deduped
--
-- Existing rows (CIQUAL 2020_07) get sane defaults: brand=NULL,
-- popularity=500 (mid-range), is_active=true.
-- ================================================================

CREATE EXTENSION IF NOT EXISTS unaccent;

-- Postgres' built-in unaccent() is STABLE (depends on an external dictionary
-- file), which forbids it inside a GENERATED column expression. We wrap it
-- in an IMMUTABLE function — the dictionary is effectively constant within
-- this database, so this is safe.
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
STRICT
AS $$ SELECT public.unaccent('public.unaccent', $1) $$;

----------------------------------------------------------------
-- 1. Schema additions
----------------------------------------------------------------
ALTER TABLE food_catalog
    ADD COLUMN IF NOT EXISTS ext_id            text,
    ADD COLUMN IF NOT EXISTS brand             text,
    ADD COLUMN IF NOT EXISTS brand_normalized  text,
    ADD COLUMN IF NOT EXISTS sugar_100g        double precision,
    ADD COLUMN IF NOT EXISTS fiber_100g        double precision,
    ADD COLUMN IF NOT EXISTS salt_100g         double precision,
    ADD COLUMN IF NOT EXISTS serving_label     text,
    ADD COLUMN IF NOT EXISTS nutriscore        char(1),
    ADD COLUMN IF NOT EXISTS nova_group        smallint,
    ADD COLUMN IF NOT EXISTS barcode           text,
    ADD COLUMN IF NOT EXISTS ciqual_fallback_id bigint REFERENCES food_catalog(id),
    ADD COLUMN IF NOT EXISTS popularity        int NOT NULL DEFAULT 500,
    ADD COLUMN IF NOT EXISTS image_url         text,
    ADD COLUMN IF NOT EXISTS is_active         boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS last_synced_at    timestamptz;

-- Backfill ext_id for existing CIQUAL rows
UPDATE food_catalog SET ext_id = id::text WHERE ext_id IS NULL;

-- Generated tsvector: name (weight A) + brand (weight B), French config, unaccented
ALTER TABLE food_catalog
    ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('french', immutable_unaccent(coalesce(name, ''))), 'A')
        || setweight(to_tsvector('french', immutable_unaccent(coalesce(brand, ''))), 'B')
    ) STORED;

-- Uniqueness: (source, ext_id) prevents duplicate seeds
ALTER TABLE food_catalog
    DROP CONSTRAINT IF EXISTS food_catalog_source_ext_id_key;
ALTER TABLE food_catalog
    ADD CONSTRAINT food_catalog_source_ext_id_key UNIQUE (source, ext_id);

-- Allow new sources beyond 'ciqual'
ALTER TABLE food_catalog
    DROP CONSTRAINT IF EXISTS food_catalog_source_check;
ALTER TABLE food_catalog
    ADD CONSTRAINT food_catalog_source_check
    CHECK (source IN ('ciqual', 'off_fr', 'off_live', 'manual_admin'));

----------------------------------------------------------------
-- 2. Indexes
----------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_food_catalog_search_vector
    ON food_catalog USING gin(search_vector);

CREATE INDEX IF NOT EXISTS idx_food_catalog_brand_trgm
    ON food_catalog USING gin(brand_normalized gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_food_catalog_barcode
    ON food_catalog(barcode) WHERE barcode IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_food_catalog_active_pop
    ON food_catalog(is_active, popularity DESC);

----------------------------------------------------------------
-- 3. Sequence for OFF-imported rows (CIQUAL ids stay <100k)
----------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS food_catalog_off_id_seq
    START WITH 1000000000
    INCREMENT BY 1
    MINVALUE 1000000000
    NO MAXVALUE;

-- Helper: allocate a batch of IDs in a single round-trip (used by the
-- `search-off-live` Edge Function before bulk-upserting OFF hits).
CREATE OR REPLACE FUNCTION nextvals_off_seq(count int)
RETURNS bigint[]
LANGUAGE sql
VOLATILE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT array_agg(nextval('food_catalog_off_id_seq')::bigint)
    FROM generate_series(1, GREATEST(count, 0));
$$;

REVOKE ALL ON FUNCTION nextvals_off_seq(int) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION nextvals_off_seq(int) TO service_role;

----------------------------------------------------------------
-- 4. RPC: unified search (FTS + trigram + popularity + dedup)
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
      AND n.tq IS NOT NULL
      AND fc.search_vector @@ n.tq
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
      AND n.nq <> ''
      AND (fc.name_normalized % n.nq OR fc.brand_normalized % n.nq)
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

-- Read access mirrors the table policy: any authenticated user.
REVOKE ALL ON FUNCTION search_food_catalog(text, int) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION search_food_catalog(text, int) TO authenticated;
