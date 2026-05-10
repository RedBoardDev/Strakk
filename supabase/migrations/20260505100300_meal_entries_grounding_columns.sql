-- ================================================================
-- meal_entries: grounding columns (V3 meal analysis)
-- ================================================================
-- All columns are nullable — existing rows are unaffected.
-- is_grounded defaults to false: legacy entries are implicitly ungrounded.
--
-- food_catalog_id: FK to unified food_catalog (CIQUAL, USDA, or OFF item).
-- grounding_source: denormalized source for display without JOIN.
-- quantity_grams: canonical quantity after unit conversion.
-- cooking_method: cooking method at time of logging (for retention factors).
-- ai_confidence: cosine similarity of the embedding match (0..1).
-- corrected_*: set when user swaps the matched item or adjusts quantity.
--   corrected_food_id has no FK — could reference any food_catalog row;
--   corrected_source disambiguates if needed.
-- ================================================================

ALTER TABLE meal_entries
    ADD COLUMN IF NOT EXISTS food_catalog_id  bigint REFERENCES food_catalog(id),
    ADD COLUMN IF NOT EXISTS grounding_source text
        CHECK (grounding_source IN ('ciqual', 'usda', 'off_fr', 'off_live')),
    ADD COLUMN IF NOT EXISTS quantity_grams   double precision
        CHECK (quantity_grams IS NULL OR quantity_grams > 0),
    ADD COLUMN IF NOT EXISTS cooking_method   text
        CHECK (cooking_method IN ('grilled','fried','steamed','baked','boiled','raw','microwaved')),
    ADD COLUMN IF NOT EXISTS is_grounded      boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS ai_confidence    double precision
        CHECK (ai_confidence IS NULL OR (ai_confidence >= 0 AND ai_confidence <= 1)),
    ADD COLUMN IF NOT EXISTS corrected_food_id   bigint,
    ADD COLUMN IF NOT EXISTS corrected_source    text
        CHECK (corrected_source IN ('ciqual', 'usda', 'off_fr', 'off_live')),
    ADD COLUMN IF NOT EXISTS corrected_quantity  double precision
        CHECK (corrected_quantity IS NULL OR corrected_quantity > 0),
    ADD COLUMN IF NOT EXISTS corrected_at        timestamptz;

-- Lookup by food item (analytics + correction tracking)
CREATE INDEX IF NOT EXISTS idx_entries_food_catalog
    ON meal_entries(food_catalog_id)
    WHERE food_catalog_id IS NOT NULL;

-- Grounded entries by user + date (TodayView nutrition display)
CREATE INDEX IF NOT EXISTS idx_entries_grounded_user_date
    ON meal_entries(user_id, log_date, is_grounded);
