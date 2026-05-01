-- ================================================================
-- Optimize search_food_catalog RPC: push LIMIT into subqueries
-- Avoids full UNION ALL materialization at scale (250k+ rows).
-- ================================================================
--
-- Before: fts_hits and trgm_hits scanned unboundedly, then UNION ALL
--         produced O(N) rows that were sorted and deduped only at the end.
-- After:  each CTE over-fetches lim*3 rows at most; the UNION ALL is
--         already small before deduplication and final ordering.
--
-- lim*3 rationale: worst-case dedup can collapse up to ~66 % of rows when
-- both arms return the same product. Over-fetching by 3x ensures the final
-- LIMIT lim is always satisfied even after heavy deduplication.
-- ================================================================

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

-- Permissions unchanged from the original migration.
REVOKE ALL ON FUNCTION search_food_catalog(text, int) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION search_food_catalog(text, int) TO authenticated;
