-- Increase statement_timeout for vector search to handle large catalogs,
-- and ANALYZE to help the query planner use the HNSW index.

ANALYZE food_catalog;

-- Recreate search function with 30s statement_timeout to prevent
-- cancellation during parallel vector searches from Edge Functions.
CREATE OR REPLACE FUNCTION search_food_catalog_vector(
    query_embedding  extensions.vector(1536),
    match_threshold  float   DEFAULT 0.4,
    match_count      int     DEFAULT 5,
    require_density  boolean DEFAULT false
)
RETURNS TABLE (
    id                    bigint,
    source                text,
    name                  text,
    similarity            double precision,
    kcal                  double precision,
    protein               double precision,
    fat                   double precision,
    carbs                 double precision,
    density               double precision,
    default_portion_grams double precision,
    serving_label         text
)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = public, extensions
SET statement_timeout = '30s'
AS $$
    SELECT
        f.id,
        f.source,
        f.name,
        (1 - (f.embedding <=> query_embedding))::double precision AS similarity,
        f.calories::double precision  AS kcal,
        f.protein::double precision   AS protein,
        f.fat::double precision       AS fat,
        f.carbs::double precision     AS carbs,
        f.density::double precision   AS density,
        f.default_portion_grams::double precision AS default_portion_grams,
        f.serving_label
    FROM food_catalog f
    WHERE f.is_active
      AND f.embedding IS NOT NULL
      AND (1 - (f.embedding <=> query_embedding)) >= match_threshold
      AND (NOT require_density OR f.density IS NOT NULL)
    ORDER BY f.embedding <=> query_embedding
    LIMIT match_count;
$$;

REVOKE ALL ON FUNCTION search_food_catalog_vector(extensions.vector, float, int, boolean) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION search_food_catalog_vector(extensions.vector, float, int, boolean) TO authenticated;
GRANT EXECUTE ON FUNCTION search_food_catalog_vector(extensions.vector, float, int, boolean) TO service_role;
