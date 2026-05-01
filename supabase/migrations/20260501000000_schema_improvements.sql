-- ================================================================
-- Strakk — Schema improvements
-- ================================================================
-- 1. Trigger: auto-create profiles row on auth.users INSERT
-- 2. Trigger: BEFORE DELETE on profiles — cleanup Vault secret
-- 3. ENUM meal_entry_source replaces TEXT + CHECK on meal_entries.source
-- 4. ENUM checkin_ai_lang replaces TEXT + CHECK on checkins.ai_summary_lang
-- ================================================================

----------------------------------------------------------------
-- 1. Auto-create profile on user signup
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO profiles (id)
    VALUES (NEW.id)
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$;

-- Drop first in case the trigger already exists from a previous attempt
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION handle_new_user();

----------------------------------------------------------------
-- 2. Cleanup Vault secret before profile deletion
--    (profiles are cascade-deleted when auth.users row is removed)
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION cleanup_profile_vault_secret()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, vault
AS $$
BEGIN
    IF OLD.hevy_api_key_secret_id IS NOT NULL THEN
        DELETE FROM vault.secrets WHERE id = OLD.hevy_api_key_secret_id;
    END IF;
    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS before_profile_delete ON profiles;

CREATE TRIGGER before_profile_delete
    BEFORE DELETE ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION cleanup_profile_vault_secret();

----------------------------------------------------------------
-- 3. ENUM for meal_entries.source
--    Replaces: TEXT + CHECK (source IN ('search','barcode','manual','text_ai','photo_ai','frequent'))
--    Constraint auto-name from init_schema is: meal_entries_source_check
----------------------------------------------------------------

-- Create the enum (idempotent via DO block)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'meal_entry_source') THEN
        CREATE TYPE meal_entry_source AS ENUM (
            'search',
            'barcode',
            'manual',
            'text_ai',
            'photo_ai',
            'frequent'
        );
    END IF;
END;
$$;

-- Drop the old CHECK constraint before altering the type (required on fresh DBs)
ALTER TABLE meal_entries DROP CONSTRAINT IF EXISTS meal_entries_source_check;

-- Migrate the column to the new type
ALTER TABLE meal_entries
    ALTER COLUMN source TYPE meal_entry_source USING source::meal_entry_source;

----------------------------------------------------------------
-- 4. ENUM for checkins.ai_summary_lang
--    Replaces: TEXT DEFAULT 'fr' + CHECK (ai_summary_lang IN ('fr', 'en'))
--    Constraint auto-name from add_checkins is: checkins_ai_summary_lang_check
----------------------------------------------------------------

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'checkin_ai_lang') THEN
        CREATE TYPE checkin_ai_lang AS ENUM ('fr', 'en');
    END IF;
END;
$$;

-- Drop the old CHECK constraint before altering the type (required on fresh DBs)
ALTER TABLE checkins DROP CONSTRAINT IF EXISTS checkins_ai_summary_lang_check;

-- Drop DEFAULT before type change (Postgres can't auto-cast text default to enum)
ALTER TABLE checkins ALTER COLUMN ai_summary_lang DROP DEFAULT;

ALTER TABLE checkins
    ALTER COLUMN ai_summary_lang TYPE checkin_ai_lang
        USING ai_summary_lang::checkin_ai_lang;

ALTER TABLE checkins ALTER COLUMN ai_summary_lang SET DEFAULT 'fr'::checkin_ai_lang;
