-- ================================================================
-- Enable pgvector for semantic food search (V3 meal analysis)
-- ================================================================
-- Required before any vector(1536) column or <=> operator.
-- Supabase installs this in the `extensions` schema; the type is
-- accessible as `vector(1536)` because `extensions` is in search_path.
-- ================================================================

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA extensions;
