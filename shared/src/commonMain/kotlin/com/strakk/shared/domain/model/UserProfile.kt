package com.strakk.shared.domain.model

/**
 * Domain representation of the `profiles` table.
 *
 * Contains user goals. All goal fields are nullable — a user can skip onboarding
 * and set them later in Settings.
 */
data class UserProfile(
    /** Supabase user ID (matches `auth.uid()`). */
    val id: String,
    /** Daily protein goal in grams. */
    val proteinGoal: Int?,
    /** Daily calorie goal in kcal. */
    val calorieGoal: Int?,
    /** Daily water goal in mL. */
    val waterGoal: Int?,
)
