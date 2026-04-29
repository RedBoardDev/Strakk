-- Split the old free-form feeling note into dedicated mental and physical fields.
-- Existing note text is preserved in mental_feeling before dropping the old column.

ALTER TABLE checkins
    ADD COLUMN IF NOT EXISTS mental_feeling text
        CHECK (mental_feeling IS NULL OR char_length(mental_feeling) <= 1000);

ALTER TABLE checkins
    ADD COLUMN IF NOT EXISTS physical_feeling text
        CHECK (physical_feeling IS NULL OR char_length(physical_feeling) <= 1000);

UPDATE checkins
SET mental_feeling = feeling_note
WHERE feeling_note IS NOT NULL
  AND mental_feeling IS NULL;

ALTER TABLE checkins
    DROP COLUMN IF EXISTS feeling_note;
