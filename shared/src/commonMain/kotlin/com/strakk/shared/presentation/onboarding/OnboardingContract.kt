package com.strakk.shared.presentation.onboarding

/**
 * Onboarding form state.
 *
 * Step 0: protein + calories goals.
 * Step 1: water goal.
 * Step 2: reminder preferences.
 *
 * Text fields are [String] to support partial/empty input.
 * Conversion to [Int] happens at submission time.
 */
data class OnboardingUiState(
    val currentStep: Int = 0,
    val proteinGoal: String = "",
    val calorieGoal: String = "",
    val waterGoal: String = "",
    val trackingReminderEnabled: Boolean = false,
    val trackingReminderTime: String = "17:00",
    val checkinReminderEnabled: Boolean = false,
    val checkinReminderDay: Int = 6,
    val checkinReminderTime: String = "10:00",
    val isSaving: Boolean = false,
) {
    val isFirstStep: Boolean get() = currentStep == 0
    val isLastStep: Boolean get() = currentStep == LAST_STEP

    companion object {
        const val LAST_STEP = 2
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

    /** Step 2: daily tracking reminder toggle. */
    data class OnTrackingReminderToggled(val enabled: Boolean) : OnboardingEvent

    /** Step 2: daily tracking reminder time selected. */
    data class OnTrackingReminderTimeChanged(val time: String) : OnboardingEvent

    /** Step 2: weekly check-in reminder toggle. */
    data class OnCheckinReminderToggled(val enabled: Boolean) : OnboardingEvent

    /** Step 2: weekly check-in reminder day selected (0 = Monday, 6 = Sunday). */
    data class OnCheckinReminderDayChanged(val day: Int) : OnboardingEvent

    /** Step 2: weekly check-in reminder time selected. */
    data class OnCheckinReminderTimeChanged(val time: String) : OnboardingEvent

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
