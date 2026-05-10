-- Extend food_catalog source constraint to allow USDA Foundation Foods and
-- Survey (FNDDS) datasets, which cover composite dishes not in SR Legacy.

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
    'usda_fndds'
  ));
