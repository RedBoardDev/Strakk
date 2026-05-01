-- ================================================================
-- Strakk — Add CHECK constraints on goals and date columns
-- ================================================================
-- M-1: profiles goal columns — guard against nonsensical values
-- M-2: meals.date and meal_entries.log_date — enforce 'yyyy-MM-dd' format
-- ================================================================

----------------------------------------------------------------
-- M-1: profiles goal constraints
----------------------------------------------------------------
ALTER TABLE profiles
  ADD CONSTRAINT IF NOT EXISTS profiles_protein_goal_check
    CHECK (protein_goal IS NULL OR (protein_goal > 0 AND protein_goal <= 500));
ALTER TABLE profiles
  ADD CONSTRAINT IF NOT EXISTS profiles_calorie_goal_check
    CHECK (calorie_goal IS NULL OR (calorie_goal > 0 AND calorie_goal <= 10000));
ALTER TABLE profiles
  ADD CONSTRAINT IF NOT EXISTS profiles_water_goal_check
    CHECK (water_goal IS NULL OR (water_goal > 0 AND water_goal <= 10000));

----------------------------------------------------------------
-- M-2: date format constraints (yyyy-MM-dd)
----------------------------------------------------------------
ALTER TABLE meals
  ADD CONSTRAINT IF NOT EXISTS meals_date_format_check
    CHECK (date ~ '^\d{4}-\d{2}-\d{2}$');
ALTER TABLE meal_entries
  ADD CONSTRAINT IF NOT EXISTS entries_log_date_format_check
    CHECK (log_date ~ '^\d{4}-\d{2}-\d{2}$');
