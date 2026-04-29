package com.strakk.shared.presentation.onboarding

/**
 * Onboarding form state.
 *
 * Step 0: protein + calories goals.
 * Step 1: water goal.
 *
 * Text fields are [String] to support partial/empty input.
 * Conversion to [Int] happens at submission time.
 */
data class OnboardingUiState(
    val currentStep: Int = 0,
    val proteinGoal: String = "",
    val calorieGoal: String = "",
    val waterGoal: String = "",
    val isSaving: Boolean = false,
) {
    val isFirstStep: Boolean get() = currentStep == 0
    val isLastStep: Boolean get() = currentStep == LAST_STEP

    companion object {
        const val LAST_STEP = 1
    }
}

/** User interactions during onboarding. */
sealed interface OnboardingEvent {
    /** Step 0: protein goal changed. */
    data class OnProteinGoalChanged(val value: String) : OnboardingEvent

    /** Step 0: calorie goal changed. */
    data class OnCalorieGoalChanged(val value: String) : OnboardingEvent

    /** Step 1: water goal changed. */
    data class OnWaterGoalChanged(val value: String) : OnboardingEvent

    /** User taps "Continue" — advances step, or saves on last step. */
    data object OnContinue : OnboardingEvent

    /** User taps back — goes to previous step. */
    data object OnBack : OnboardingEvent
}

/** One-shot side effects consumed by the UI layer. */
sealed interface OnboardingEffect {
    /** Profile created successfully — navigate to Home. */
    data object NavigateToHome : OnboardingEffect

    /** Display an error message. */
    data class ShowError(val message: String) : OnboardingEffect
}
