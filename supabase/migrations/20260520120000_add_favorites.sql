-- ================================================================
-- favorite_foods + favorite_meals (+ recents helpers)
-- ----------------------------------------------------------------
-- Adds the favorites mechanism behind the new Search drawer V2:
--   * favorite_foods — denormalized per-user food template
--   * favorite_meals — denormalized per-user meal template (with items_json)
--   * recent_meals_v1(user_id, days, max_rows)  — RPC: distinct meals by name
--   * recent_foods_v1(user_id, days, max_rows)  — RPC: distinct foods by name
-- Both tables have RLS limited to the owner. Favorites survive deletion of
-- their source rows (no FK to meals / meal_entries).
-- ================================================================

----------------------------------------------------------------
-- 1. favorite_foods
----------------------------------------------------------------
CREATE TABLE favorite_foods (
    id              uuid             PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid             NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name            text             NOT NULL CHECK (char_length(name) BETWEEN 1 AND 100),
    name_normalized text             NOT NULL,
    protein         double precision NOT NULL DEFAULT 0 CHECK (protein >= 0 AND protein <= 500),
    calories        double precision NOT NULL DEFAULT 0 CHECK (calories >= 0 AND calories <= 5000),
    fat             double precision CHECK (fat IS NULL OR (fat >= 0 AND fat <= 500)),
    carbs           double precision CHECK (carbs IS NULL OR (carbs >= 0 AND carbs <= 500)),
    quantity        text             CHECK (quantity IS NULL OR char_length(quantity) <= 50),
    food_catalog_id bigint           REFERENCES food_catalog(id) ON DELETE SET NULL,
    created_at      timestamptz      NOT NULL DEFAULT now(),
    UNIQUE (user_id, name_normalized)
);

CREATE INDEX idx_favorite_foods_user_created ON favorite_foods(user_id, created_at DESC);

ALTER TABLE favorite_foods ENABLE ROW LEVEL SECURITY;

CREATE POLICY "favorite_foods_owner_all" ON favorite_foods
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

----------------------------------------------------------------
-- 2. favorite_meals
----------------------------------------------------------------
CREATE TABLE favorite_meals (
    id              uuid             PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid             NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name            text             NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    items_json      jsonb            NOT NULL,
    -- Originating meal id, if any. Not an FK — favorite survives meal deletion.
    source_meal_id  uuid,
    created_at      timestamptz      NOT NULL DEFAULT now()
);

CREATE INDEX idx_favorite_meals_user_created ON favorite_meals(user_id, created_at DESC);
CREATE INDEX idx_favorite_meals_user_source  ON favorite_meals(user_id, source_meal_id)
    WHERE source_meal_id IS NOT NULL;

ALTER TABLE favorite_meals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "favorite_meals_owner_all" ON favorite_meals
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

----------------------------------------------------------------
-- 3. recent_meals RPC — distinct meals by name (last N days)
----------------------------------------------------------------
-- Returns the latest occurrence per meal name within the requested window for
-- the caller. The client uses this to populate the "Recent meals" section of
-- the search drawer. Items are aggregated as JSONB so we keep the call cheap
-- (one row per meal).
CREATE OR REPLACE FUNCTION recent_meals_v1(
    days_window integer DEFAULT 30,
    max_rows    integer DEFAULT 20
)
RETURNS TABLE (
    meal_id    uuid,
    name       text,
    created_at timestamptz,
    items      jsonb
)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = public, pg_temp
AS $$
    WITH latest AS (
        SELECT DISTINCT ON (lower(m.name))
            m.id AS meal_id,
            m.name,
            m.created_at
        FROM meals m
        WHERE m.user_id = auth.uid()
          AND m.created_at >= now() - make_interval(days => days_window)
        ORDER BY lower(m.name), m.created_at DESC
    )
    SELECT
        l.meal_id,
        l.name,
        l.created_at,
        COALESCE(
            (SELECT jsonb_agg(jsonb_build_object(
                'name',     e.name,
                'protein',  e.protein,
                'calories', e.calories,
                'fat',      e.fat,
                'carbs',    e.carbs,
                'quantity', e.quantity
            ) ORDER BY e.created_at)
             FROM meal_entries e
             WHERE e.meal_id = l.meal_id),
            '[]'::jsonb
        ) AS items
    FROM latest l
    ORDER BY l.created_at DESC
    LIMIT max_rows;
$$;

GRANT EXECUTE ON FUNCTION recent_meals_v1(integer, integer) TO authenticated;

----------------------------------------------------------------
-- 4. recent_foods RPC — distinct foods by normalized name (last N days)
----------------------------------------------------------------
-- Pure-recency variant of the existing frequent-items query: deduplicates by
-- normalized name and sorts by max(created_at) DESC (no frequency weight).
CREATE OR REPLACE FUNCTION recent_foods_v1(
    days_window integer DEFAULT 60,
    max_rows    integer DEFAULT 30
)
RETURNS TABLE (
    name_normalized text,
    name            text,
    protein         double precision,
    calories        double precision,
    fat             double precision,
    carbs           double precision,
    quantity        text,
    last_used_at    timestamptz
)
LANGUAGE sql
STABLE
SECURITY INVOKER
SET search_path = public, pg_temp
AS $$
    WITH latest AS (
        SELECT DISTINCT ON (lower(immutable_unaccent(coalesce(e.name, ''))))
            lower(immutable_unaccent(coalesce(e.name, ''))) AS name_normalized,
            e.name,
            e.protein,
            e.calories,
            e.fat,
            e.carbs,
            e.quantity,
            e.created_at AS last_used_at
        FROM meal_entries e
        WHERE e.user_id = auth.uid()
          AND e.name IS NOT NULL
          AND char_length(trim(e.name)) > 0
          AND e.created_at >= now() - make_interval(days => days_window)
        ORDER BY
            lower(immutable_unaccent(coalesce(e.name, ''))),
            e.created_at DESC
    )
    SELECT *
    FROM latest
    ORDER BY last_used_at DESC
    LIMIT max_rows;
$$;

GRANT EXECUTE ON FUNCTION recent_foods_v1(integer, integer) TO authenticated;

----------------------------------------------------------------
-- 5. Force PostgREST to reload the schema cache
----------------------------------------------------------------
NOTIFY pgrst, 'reload schema';
