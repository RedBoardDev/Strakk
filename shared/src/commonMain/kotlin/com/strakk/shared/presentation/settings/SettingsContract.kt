package com.strakk.shared.presentation.settings

// =============================================================================
// UiState
// =============================================================================

/**
 * Settings screen state.
 *
 * Goal fields are [String] (not [Int]?) to support direct two-way binding
 * with text fields — an empty string means "no goal set".
 *
 * Reminder toggles are separate booleans so the UI can show/hide the
 * time/day pickers independently of their stored values.
 */
sealed interface SettingsUiState {
    /** Initial loading while profile is fetched. */
    data object Loading : SettingsUiState

    /**
     * Profile data is ready to display and edit.
     *
     * @param email User email from auth (read-only display). Null if unavailable.
     * @param proteinGoal Daily protein goal as editable text (empty = no goal).
     * @param calorieGoal Daily calorie goal as editable text (empty = no goal).
     * @param waterGoal Daily water goal as editable text (empty = no goal).
     * @param trackingReminderEnabled Whether the daily tracking reminder is active.
     * @param trackingReminderTime Time for the tracking reminder in "HH:mm" format.
     * @param checkinReminderEnabled Whether the weekly check-in reminder is active.
     * @param checkinReminderDay Day for the check-in reminder (0=Mon..6=Sun).
     * @param checkinReminderTime Time for the check-in reminder in "HH:mm" format.
     */
    data class Ready(
        val email: String?,
        val proteinGoal: String,
        val calorieGoal: String,
        val waterGoal: String,
        val trackingReminderEnabled: Boolean,
        val trackingReminderTime: String,
        val checkinReminderEnabled: Boolean,
        val checkinReminderDay: Int,
        val checkinReminderTime: String,
        /** Hevy API key as editable text (empty = not configured). */
        val hevyApiKey: String,
    ) : SettingsUiState
}

// =============================================================================
// Events (UI -> ViewModel)
// =============================================================================

/** User interactions on the Settings screen. */
sealed interface SettingsEvent {
    /** User edits the daily protein goal text field. */
    data class OnProteinGoalChanged(val value: String) : SettingsEvent

    /** User edits the daily calorie goal text field. */
    data class OnCalorieGoalChanged(val value: String) : SettingsEvent

    /** User edits the daily water goal text field. */
    data class OnWaterGoalChanged(val value: String) : SettingsEvent

    /** User toggles the daily tracking reminder on/off. */
    data class OnTrackingReminderEnabledChanged(val enabled: Boolean) : SettingsEvent

    /** User picks a new time for the daily tracking reminder. */
    data class OnTrackingReminderTimeChanged(val time: String) : SettingsEvent

    /** User toggles the weekly check-in reminder on/off. */
    data class OnCheckinReminderEnabledChanged(val enabled: Boolean) : SettingsEvent

    /** User picks a new day for the weekly check-in reminder. */
    data class OnCheckinReminderDayChanged(val day: Int) : SettingsEvent

    /** User picks a new time for the weekly check-in reminder. */
    data class OnCheckinReminderTimeChanged(val time: String) : SettingsEvent

    /** User edits the Hevy API key field. */
    data class OnHevyApiKeyChanged(val value: String) : SettingsEvent

    /** User taps sign out. */
    data object OnSignOut : SettingsEvent
}

// =============================================================================
// Effects (ViewModel -> UI, one-shot)
// =============================================================================

/** One-shot side effects consumed by the UI layer. */
sealed interface SettingsEffect {
    /** Display an error message (snackbar or inline). */
    data class ShowError(val message: String) : SettingsEffect
}
