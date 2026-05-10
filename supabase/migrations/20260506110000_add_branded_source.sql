-- Add usda_branded to allowed sources and reactivate Foundation Foods
-- (will be re-imported with correct kcal nutrient number).

ALTER TABLE food_catalog DROP CONSTRAINT IF EXISTS food_catalog_source_check;

ALTER TABLE food_catalog
  ADD CONSTRAINT food_catalog_source_check
  CHECK (source IN (
    'ciqual',
    'off_fr',
    'off_live',
    'manual_admin',
    'usda',
    'usda_foundation',
    'usda_fndds',
    'usda_branded'
  ));

-- Reactivate Foundation Foods — the fix script will merge correct macros.
UPDATE food_catalog SET is_active = true WHERE source = 'usda_foundation';
