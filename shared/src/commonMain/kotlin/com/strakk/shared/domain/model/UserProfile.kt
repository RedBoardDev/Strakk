package com.strakk.shared.domain.model

/**
 * Domain representation of the `profiles` table.
 *
 * Contains user goals and notification preferences.
 * All goal/reminder fields are nullable — a user can skip onboarding
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
    /** Daily tracking reminder time in "HH:mm" format. null = disabled. */
    val reminderTrackingTime: String?,
    /** Weekly check-in reminder day (0 = Monday, 6 = Sunday). null = disabled. */
    val reminderCheckinDay: Int?,
    /** Weekly check-in reminder time in "HH:mm" format. null = disabled. */
    val reminderCheckinTime: String?,
    /** Hevy API key for exporting workout sessions. null = not configured. */
    val hevyApiKey: String? = null,
)
