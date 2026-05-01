-- ================================================================
-- Strakk — Security hardening (Tier-0 audit fixes)
-- ================================================================
-- Changes:
--   * Fix immutable_unaccent() — single canonical definition
--   * Drop profiles.hevy_api_key plaintext column (Vault only)
--   * Update get_hevy_api_key() — remove plaintext fallback
--   * Update save_hevy_api_key() — remove hevy_api_key NULL writes
--   * REVOKE EXECUTE FROM PUBLIC before GRANT on vault RPCs
-- ================================================================

----------------------------------------------------------------
-- 0.4 — Fix immutable_unaccent (was defined twice with different
--        implementations across init_schema and extend_food_catalog)
----------------------------------------------------------------
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
STRICT
SET search_path = extensions, public, pg_temp
AS $$
    SELECT unaccent($1);
$$;

----------------------------------------------------------------
-- 0.2a — Update get_hevy_api_key() to remove plaintext fallback.
--         Vault-only from now on.
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

    IF v_secret_id IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT decrypted_secret INTO v_key
    FROM vault.decrypted_secrets
    WHERE id = v_secret_id;

    RETURN v_key;
END;
$$;

----------------------------------------------------------------
-- 0.2b — Update save_hevy_api_key() — no more hevy_api_key ref
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
        v_secret_id := vault.create_secret(
            plain_key,
            'hevy_api_key_' || v_user_id::text,
            'Hevy API key for user ' || v_user_id::text
        );
        UPDATE profiles
        SET hevy_api_key_secret_id = v_secret_id
        WHERE id = v_user_id;
    ELSE
        PERFORM vault.update_secret(v_secret_id, plain_key);
    END IF;
END;
$$;

----------------------------------------------------------------
-- 0.2c — REVOKE from PUBLIC, then GRANT to authenticated only
----------------------------------------------------------------
REVOKE ALL ON FUNCTION get_hevy_api_key() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION get_hevy_api_key() TO authenticated;

REVOKE ALL ON FUNCTION save_hevy_api_key(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION save_hevy_api_key(text) TO authenticated;

----------------------------------------------------------------
-- 0.2d — Drop the plaintext column (all keys migrated to Vault
--         by the previous migration 20260428000000)
----------------------------------------------------------------
ALTER TABLE profiles DROP COLUMN IF EXISTS hevy_api_key;
