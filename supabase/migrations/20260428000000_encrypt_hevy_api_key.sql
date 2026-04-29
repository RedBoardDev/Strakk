-- ================================================================
-- Strakk — Encrypt Hevy API key at rest (Supabase Vault)
-- ================================================================
-- Changes:
--   * Add hevy_api_key_secret_id (uuid) to profiles
--   * RPC save_hevy_api_key(plain_key) — stores in Vault, saves secret ID
--   * RPC get_hevy_api_key() — reads from Vault via secret ID
--   * Migrate existing plaintext keys to Vault
-- ================================================================

----------------------------------------------------------------
-- 1. New column — stores the Vault secret UUID
----------------------------------------------------------------
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS hevy_api_key_secret_id uuid;

----------------------------------------------------------------
-- 2. save_hevy_api_key(plain_key text)
--    Creates or updates the Vault secret for the calling user.
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION save_hevy_api_key(plain_key text)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, vault
AS $$
DECLARE
    v_user_id  uuid;
    v_secret_id uuid;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT hevy_api_key_secret_id INTO v_secret_id
    FROM profiles
    WHERE id = v_user_id;

    IF v_secret_id IS NULL THEN
        -- Create a new Vault secret
        v_secret_id := vault.create_secret(
            plain_key,
            'hevy_api_key_' || v_user_id::text,
            'Hevy API key for user ' || v_user_id::text
        );
        UPDATE profiles
        SET hevy_api_key_secret_id = v_secret_id,
            hevy_api_key = NULL
        WHERE id = v_user_id;
    ELSE
        -- Update the existing secret
        PERFORM vault.update_secret(v_secret_id, plain_key);
        UPDATE profiles
        SET hevy_api_key = NULL
        WHERE id = v_user_id;
    END IF;
END;
$$;

----------------------------------------------------------------
-- 3. get_hevy_api_key()
--    Returns the decrypted Hevy API key for the calling user.
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION get_hevy_api_key()
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, vault
AS $$
DECLARE
    v_user_id   uuid;
    v_secret_id uuid;
    v_key       text;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Not authenticated';
    END IF;

    SELECT hevy_api_key_secret_id INTO v_secret_id
    FROM profiles
    WHERE id = v_user_id;

    -- Fall back to plaintext column during transition
    IF v_secret_id IS NULL THEN
        SELECT hevy_api_key INTO v_key
        FROM profiles
        WHERE id = v_user_id;
        RETURN v_key;
    END IF;

    SELECT decrypted_secret INTO v_key
    FROM vault.decrypted_secrets
    WHERE id = v_secret_id;

    RETURN v_key;
END;
$$;

----------------------------------------------------------------
-- 4. Grants — authenticated users only
----------------------------------------------------------------
GRANT EXECUTE ON FUNCTION save_hevy_api_key(text) TO authenticated;
GRANT EXECUTE ON FUNCTION get_hevy_api_key()       TO authenticated;

----------------------------------------------------------------
-- 5. Migrate existing plaintext keys to Vault
--    Runs as the migration user (postgres), uses service role
--    context via a DO block with explicit role switching.
----------------------------------------------------------------
DO $$
DECLARE
    r RECORD;
    v_secret_id uuid;
BEGIN
    FOR r IN
        SELECT id, hevy_api_key
        FROM profiles
        WHERE hevy_api_key IS NOT NULL
          AND hevy_api_key_secret_id IS NULL
    LOOP
        v_secret_id := vault.create_secret(
            r.hevy_api_key,
            'hevy_api_key_' || r.id::text,
            'Hevy API key for user ' || r.id::text
        );
        UPDATE profiles
        SET hevy_api_key_secret_id = v_secret_id,
            hevy_api_key = NULL
        WHERE id = r.id;
    END LOOP;
END;
$$;
