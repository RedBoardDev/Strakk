-- ================================================================
-- Strakk — Initial schema (from-scratch, pre-prod)
-- ================================================================
-- Meal Refonte v2 baseline :
--   * profiles             — user settings, goals, reminders, hevy key
--   * water_entries        — daily water intake (unchanged model)
--   * meals                — meal container (Draft → Processed server-side = Processed only)
--   * meal_entries         — atomic food items, can be orphan (quick-add) or attached (meal_id)
--   * food_catalog         — CIQUAL reference data (public read for authenticated users)
--   * storage bucket meal-photos — private photos, {user_id}/{...} path enforced via RLS
-- ================================================================

----------------------------------------------------------------
-- 0. Extensions
----------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto;     -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pg_trgm;      -- trigram search for food_catalog
CREATE EXTENSION IF NOT EXISTS unaccent;     -- FR accent normalisation

----------------------------------------------------------------
-- 1. Shared utilities
----------------------------------------------------------------

-- Auto-update updated_at on row mutation
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Immutable wrapper around unaccent() so it can be used in expression indexes.
-- The extension's unaccent() is STABLE (depends on the unaccent dictionary) and
-- therefore rejected by Postgres in index expressions. We rely on the default
-- system-provided dictionary and assert immutability explicitly.
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
SET search_path = extensions, public, pg_temp
AS $$
    SELECT unaccent($1);
$$;

----------------------------------------------------------------
-- 2. profiles — one row per user (onboarding)
----------------------------------------------------------------
CREATE TABLE profiles (
    id                      uuid        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    protein_goal            integer,
    calorie_goal            integer,
    water_goal              integer,
    reminder_tracking_time  time,
    reminder_checkin_day    smallint,
    reminder_checkin_time   time,
    hevy_api_key            text,
    created_at              timestamptz NOT NULL DEFAULT now(),
    updated_at              timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "profiles_owner_all" ON profiles
    USING (id = auth.uid())
    WITH CHECK (id = auth.uid());

CREATE TRIGGER profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

----------------------------------------------------------------
-- 3. water_entries — daily water intake logs
----------------------------------------------------------------
CREATE TABLE water_entries (
    id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    log_date   date        NOT NULL,
    amount     integer     NOT NULL CHECK (amount > 0 AND amount <= 5000),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_water_user_date ON water_entries(user_id, log_date);

ALTER TABLE water_entries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "water_owner_all" ON water_entries
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

----------------------------------------------------------------
-- 4. meals — meal container (Processed state server-side)
----------------------------------------------------------------
CREATE TABLE meals (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date        text        NOT NULL,                                -- 'yyyy-MM-dd' (user local)
    name        text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_meals_user_date    ON meals(user_id, date);
CREATE INDEX idx_meals_user_created ON meals(user_id, created_at DESC);

ALTER TABLE meals ENABLE ROW LEVEL SECURITY;

CREATE POLICY "meals_owner_all" ON meals
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

----------------------------------------------------------------
-- 5. meal_entries — atomic food items (orphan = quick-add, or meal_id attached)
----------------------------------------------------------------
CREATE TABLE meal_entries (
    id              uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    meal_id         uuid        REFERENCES meals(id) ON DELETE CASCADE,  -- NULL = orphan quick-add
    log_date        text        NOT NULL,                                -- 'yyyy-MM-dd' (user local)
    name            text        CHECK (name IS NULL OR char_length(name) BETWEEN 1 AND 100),
    protein         double precision NOT NULL DEFAULT 0 CHECK (protein >= 0 AND protein <= 500),
    calories        double precision NOT NULL DEFAULT 0 CHECK (calories >= 0 AND calories <= 5000),
    fat             double precision CHECK (fat IS NULL OR (fat >= 0 AND fat <= 500)),
    carbs           double precision CHECK (carbs IS NULL OR (carbs >= 0 AND carbs <= 500)),
    quantity        text        CHECK (quantity IS NULL OR char_length(quantity) <= 50),
    source          text        NOT NULL CHECK (source IN ('search','barcode','manual','text_ai','photo_ai','frequent')),
    breakdown_json  jsonb,                                              -- BreakdownItem[] or NULL
    photo_path      text,                                               -- Supabase Storage path, ex: {user_id}/{mealId_or_orphan}/{entry_id}.jpg
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_entries_user_logdate ON meal_entries(user_id, log_date);
CREATE INDEX idx_entries_meal         ON meal_entries(meal_id) WHERE meal_id IS NOT NULL;
CREATE INDEX idx_entries_user_name    ON meal_entries(user_id, lower(immutable_unaccent(coalesce(name, ''))));
CREATE INDEX idx_entries_user_created ON meal_entries(user_id, created_at DESC);

ALTER TABLE meal_entries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "entries_owner_all" ON meal_entries
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());

----------------------------------------------------------------
-- 6. food_catalog — public reference (CIQUAL / future sources)
----------------------------------------------------------------
CREATE TABLE food_catalog (
    id                      bigint       PRIMARY KEY,                 -- aligned on ANSES alim_code (e.g. 25400)
    name                    text         NOT NULL,
    name_normalized         text         NOT NULL,                    -- lower + unaccent + trim
    protein                 double precision NOT NULL,                -- g / 100g
    calories                double precision NOT NULL,                -- kcal / 100g
    fat                     double precision,                         -- g / 100g
    carbs                   double precision,                         -- g / 100g
    default_portion_grams   double precision NOT NULL DEFAULT 100,
    source                  text         NOT NULL DEFAULT 'ciqual',   -- future: 'off', 'manual_admin'
    source_version          text,                                     -- e.g. 'ciqual_2020_07'
    created_at              timestamptz  NOT NULL DEFAULT now()
);

-- Trigram index for fuzzy search (tolerates typos)
CREATE INDEX idx_food_catalog_name_trgm   ON food_catalog USING gin (name_normalized gin_trgm_ops);
CREATE INDEX idx_food_catalog_name_prefix ON food_catalog (name_normalized text_pattern_ops);

ALTER TABLE food_catalog ENABLE ROW LEVEL SECURITY;

-- Read-only for any authenticated user (public reference data)
CREATE POLICY "food_catalog_read_authenticated" ON food_catalog
    FOR SELECT
    USING (auth.role() = 'authenticated');

-- No INSERT/UPDATE/DELETE policies: only server-side seeds (service role) write here.

----------------------------------------------------------------
-- 7. Storage bucket meal-photos — private, user-scoped
----------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public)
VALUES ('meal-photos', 'meal-photos', false)
ON CONFLICT (id) DO NOTHING;

-- Path pattern enforced: {user_id}/{draft_id_or_meal_id}/{entry_id}.jpg
-- (storage.foldername extracts path segments, [1] = first folder = user_id)

CREATE POLICY "meal_photos_owner_select" ON storage.objects
    FOR SELECT
    USING (
        bucket_id = 'meal-photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

CREATE POLICY "meal_photos_owner_insert" ON storage.objects
    FOR INSERT
    WITH CHECK (
        bucket_id = 'meal-photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

CREATE POLICY "meal_photos_owner_update" ON storage.objects
    FOR UPDATE
    USING (
        bucket_id = 'meal-photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

CREATE POLICY "meal_photos_owner_delete" ON storage.objects
    FOR DELETE
    USING (
        bucket_id = 'meal-photos'
        AND (storage.foldername(name))[1] = auth.uid()::text
    );
