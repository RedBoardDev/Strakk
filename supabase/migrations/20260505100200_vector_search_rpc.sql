-- ================================================================
-- search_food_catalog_vector — semantic food matching (V3)
-- ================================================================
-- Used by the `ground-meal-items` edge function to match AI-identified
-- food names against the full food_catalog (CIQUAL + USDA + OFF).
--
-- Returns full macro data inline — avoids a second SELECT per matched
-- item in the grounding pipeline.
--
-- require_density: when true, only returns items with a density value.
-- Set to true when the AI predicted a volume unit (cups, ml, tbsp)
-- so that the grounding can convert volume → grams via density.
--
-- The IVFFlat index (created by tools/import-embeddings/ after data
-- load) accelerates the <=> operator. Without it, Postgres falls back
-- to sequential scan — still correct, slightly slower on 11K rows.
-- ================================================================

CREATE OR REPLACE FUNCTION search_food_catalog_vector(
    query_embedding  extensions.vector(1536),
    match_threshold  float   DEFAULT 0.4,
    match_count      int     DEFAULT 5,
    require_density  boolean DEFAULT false
)
-- Density is cast to double precision to avoid PostgREST returning numeric as
-- string (which would break Number arithmetic in Deno / supabase-kt). Same for
-- protein/calories/fat/carbs to ensure consistent typing across the wire.
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
