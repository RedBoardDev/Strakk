-- ================================================================
-- Strakk — Add CHECK constraints on goals and date columns
-- ================================================================
-- M-1: profiles goal columns — guard against nonsensical values
-- M-2: meals.date and meal_entries.log_date — enforce 'yyyy-MM-dd' format
-- ================================================================

----------------------------------------------------------------
-- M-1: profiles goal constraints
----------------------------------------------------------------
DO $$ BEGIN
  ALTER TABLE profiles ADD CONSTRAINT profiles_protein_goal_check
    CHECK (protein_goal IS NULL OR (protein_goal > 0 AND protein_goal <= 500));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TABLE profiles ADD CONSTRAINT profiles_calorie_goal_check
    CHECK (calorie_goal IS NULL OR (calorie_goal > 0 AND calorie_goal <= 10000));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TABLE profiles ADD CONSTRAINT profiles_water_goal_check
    CHECK (water_goal IS NULL OR (water_goal > 0 AND water_goal <= 10000));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

----------------------------------------------------------------
-- M-2: date format constraints (yyyy-MM-dd)
----------------------------------------------------------------
DO $$ BEGIN
  ALTER TABLE meals ADD CONSTRAINT meals_date_format_check
    CHECK (date ~ '^\d{4}-\d{2}-\d{2}$');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TABLE meal_entries ADD CONSTRAINT entries_log_date_format_check
    CHECK (log_date ~ '^\d{4}-\d{2}-\d{2}$');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
