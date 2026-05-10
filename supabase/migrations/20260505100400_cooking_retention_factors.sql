-- ================================================================
-- cooking_retention_factors — macro adjustments by cooking method
-- ================================================================
-- Factors from USDA Nutrient Retention Factors table (Release 6).
-- factor < 1.0 = nutrient lost during cooking.
-- factor > 1.0 = nutrient added/concentrated (e.g. fat absorption when frying).
-- factor = 1.0 = no change.
--
-- Applied by AdjustGroundedItemUseCase when cooking_method is set:
--   adjusted_value = db_value_per_100g * (quantity_grams / 100) * factor
--
-- Read-only reference data: authenticated users can SELECT, no mutations.
-- ================================================================

CREATE TABLE IF NOT EXISTS cooking_retention_factors (
    id       serial           PRIMARY KEY,
    method   text             NOT NULL,
    nutrient text             NOT NULL CHECK (nutrient IN ('kcal', 'protein', 'fat', 'carbs')),
    factor   double precision NOT NULL CHECK (factor > 0),
    UNIQUE (method, nutrient)
);

ALTER TABLE cooking_retention_factors ENABLE ROW LEVEL SECURITY;

CREATE POLICY "retention_factors_read_authenticated"
    ON cooking_retention_factors
    FOR SELECT
    USING (auth.role() = 'authenticated');

INSERT INTO cooking_retention_factors (method, nutrient, factor) VALUES
    -- Grilled: fat drips off, protein slightly denatured
    ('grilled',    'kcal',    0.92),
    ('grilled',    'protein', 0.89),
    ('grilled',    'fat',     0.85),
    ('grilled',    'carbs',   1.00),
    -- Fried: fat absorbed from oil, higher kcal
    ('fried',      'kcal',    1.10),
    ('fried',      'protein', 0.88),
    ('fried',      'fat',     1.45),
    ('fried',      'carbs',   1.00),
    -- Steamed: minimal nutrient loss
    ('steamed',    'kcal',    0.97),
    ('steamed',    'protein', 0.92),
    ('steamed',    'fat',     0.95),
    ('steamed',    'carbs',   0.98),
    -- Boiled: water-soluble nutrients leach out
    ('boiled',     'kcal',    0.90),
    ('boiled',     'protein', 0.90),
    ('boiled',     'fat',     0.85),
    ('boiled',     'carbs',   0.95),
    -- Baked: moderate loss
    ('baked',      'kcal',    0.94),
    ('baked',      'protein', 0.91),
    ('baked',      'fat',     0.88),
    ('baked',      'carbs',   1.00),
    -- Raw: no change (reference baseline)
    ('raw',        'kcal',    1.00),
    ('raw',        'protein', 1.00),
    ('raw',        'fat',     1.00),
    ('raw',        'carbs',   1.00),
    -- Microwaved: similar to steamed
    ('microwaved', 'kcal',    0.95),
    ('microwaved', 'protein', 0.93),
    ('microwaved', 'fat',     0.94),
    ('microwaved', 'carbs',   0.97)
ON CONFLICT (method, nutrient) DO NOTHING;
