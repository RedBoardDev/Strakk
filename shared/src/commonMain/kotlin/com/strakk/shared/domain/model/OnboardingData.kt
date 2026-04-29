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
)
