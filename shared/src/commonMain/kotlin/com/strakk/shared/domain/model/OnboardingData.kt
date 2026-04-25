package com.strakk.shared.domain.model

/**
 * Data collected during the onboarding flow (Goals Setup).
 *
 * Passed to [com.strakk.shared.domain.repository.ProfileRepository.createProfile]
 * to create the initial `profiles` row. All fields are nullable because the user
 * can skip every step.
 */
data class OnboardingData(
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
)
