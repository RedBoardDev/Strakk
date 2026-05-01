-- Onboarding v2: extend profiles with biometrics, fitness goals, and activity data.
-- These fields are collected during onboarding and used by the AI goals calculator.

ALTER TABLE profiles
  ADD COLUMN IF NOT EXISTS weight_kg numeric(5,1),
  ADD COLUMN IF NOT EXISTS height_cm integer,
  ADD COLUMN IF NOT EXISTS birth_date date,
  ADD COLUMN IF NOT EXISTS biological_sex text,
  ADD COLUMN IF NOT EXISTS fitness_goal text,
  ADD COLUMN IF NOT EXISTS training_frequency smallint,
  ADD COLUMN IF NOT EXISTS training_types text[],
  ADD COLUMN IF NOT EXISTS training_intensity text,
  ADD COLUMN IF NOT EXISTS daily_activity_level text,
  ADD COLUMN IF NOT EXISTS fat_goal integer,
  ADD COLUMN IF NOT EXISTS carb_goal integer,
  ADD COLUMN IF NOT EXISTS onboarding_completed boolean NOT NULL DEFAULT false;

-- Enum-like check constraints
ALTER TABLE profiles
  ADD CONSTRAINT chk_biological_sex
    CHECK (biological_sex IN ('male', 'female', 'unspecified')),
  ADD CONSTRAINT chk_fitness_goal
    CHECK (fitness_goal IN ('lose_fat', 'gain_muscle', 'maintain', 'just_track')),
  ADD CONSTRAINT chk_training_intensity
    CHECK (training_intensity IN ('light', 'moderate', 'intense')),
  ADD CONSTRAINT chk_daily_activity_level
    CHECK (daily_activity_level IN ('sedentary', 'moderately_active', 'very_active'));

-- Range constraints
ALTER TABLE profiles
  ADD CONSTRAINT chk_weight_range CHECK (weight_kg BETWEEN 20 AND 300),
  ADD CONSTRAINT chk_height_range CHECK (height_cm BETWEEN 80 AND 250),
  ADD CONSTRAINT chk_training_frequency_range CHECK (training_frequency BETWEEN 0 AND 7),
  ADD CONSTRAINT chk_fat_goal_range CHECK (fat_goal BETWEEN 0 AND 500),
  ADD CONSTRAINT chk_carb_goal_range CHECK (carb_goal BETWEEN 0 AND 1000);

-- Backfill: mark all existing users as having completed onboarding
UPDATE profiles SET onboarding_completed = true WHERE onboarding_completed = false;
