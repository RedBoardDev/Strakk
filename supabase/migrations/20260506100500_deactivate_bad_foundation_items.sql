-- Deactivate Foundation Foods items with zero calories.
-- These were imported with wrong nutrient mapping (Foundation Foods
-- use a different nutrient format in the FDC API). They pollute
-- disambiguation by offering 0-calorie matches for real foods.
-- Can be reactivated after fixing the nutrient import.

UPDATE food_catalog
SET is_active = false
WHERE source = 'usda_foundation'
  AND calories = 0;
