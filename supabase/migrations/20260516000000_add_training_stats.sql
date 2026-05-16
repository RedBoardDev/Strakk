-- Add training_stats JSONB column to checkins
-- Stores cached Hevy workout data to avoid re-fetching on every view.
ALTER TABLE checkins
    ADD COLUMN IF NOT EXISTS training_stats jsonb;

COMMENT ON COLUMN checkins.training_stats IS 'Cached Hevy training stats for the check-in period (JSONB). Null means not yet loaded.';
