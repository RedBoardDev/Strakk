package com.strakk.shared.presentation.checkin

import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.domain.model.NutritionSummary

enum class WizardStep { Dates, Measurements, Feelings, Photos, Summary }

sealed interface CheckInWizardUiState {
    data object Loading : CheckInWizardUiState

    data class Ready(
        val isEditMode: Boolean,
        val currentStep: WizardStep,

        // Step 1 — Dates
        val weekLabel: String,
        val availableWeeks: List<WeekOption>,
        val coveredDates: Set<String>,
        val weekDays: List<DayOption>,
        val existingCheckInId: String?,

        // Step 2 — Measurements (stored as raw strings to support partial decimal input)
        val weight: String,
        val shoulders: String,
        val chest: String,
        val armLeft: String,
        val armRight: String,
        val waist: String,
        val hips: String,
        val thighLeft: String,
        val thighRight: String,
        val delta: CheckInDelta?,

        // Step 3 — Feelings
        val selectedTags: Set<String>,
        val mentalFeeling: String,
        val physicalFeeling: String,

        // Step 4 — Photos
        val photos: List<WizardPhoto>,

        // Step 5 — Summary
        val nutritionSummary: NutritionSummary?,
        val nutritionLoading: Boolean,
        val saving: Boolean,
    ) : CheckInWizardUiState {
        val canGoNext: Boolean get() = when (currentStep) {
            WizardStep.Dates -> coveredDates.isNotEmpty() && existingCheckInId == null
            WizardStep.Measurements -> true
            WizardStep.Feelings -> true
            WizardStep.Photos -> true
            WizardStep.Summary -> !saving
        }
    }
}

data class WeekOption(
    val weekLabel: String,
    val displayLabel: String,
    val startDate: String,
    val endDate: String,
)

data class DayOption(
    val date: String,
    val displayLabel: String,
    val selected: Boolean,
)

sealed interface WizardPhoto {
    val id: String

    data class Remote(
        override val id: String,
        val storagePath: String,
        val signedUrl: String,
    ) : WizardPhoto

    data class Local(
        override val id: String,
        val imageData: ByteArray,
    ) : WizardPhoto {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Local && id == other.id)
        override fun hashCode(): Int = id.hashCode()
    }
}

sealed interface CheckInWizardEvent {
    // Navigation
    data object OnNext : CheckInWizardEvent
    data object OnBack : CheckInWizardEvent
    data object OnCancel : CheckInWizardEvent

    // Step 1
    data class OnSelectWeek(val weekLabel: String) : CheckInWizardEvent
    data class OnToggleDate(val date: String) : CheckInWizardEvent

    // Step 2
    data class OnWeightChanged(val value: String) : CheckInWizardEvent
    data class OnShouldersChanged(val value: String) : CheckInWizardEvent
    data class OnChestChanged(val value: String) : CheckInWizardEvent
    data class OnArmLeftChanged(val value: String) : CheckInWizardEvent
    data class OnArmRightChanged(val value: String) : CheckInWizardEvent
    data class OnWaistChanged(val value: String) : CheckInWizardEvent
    data class OnHipsChanged(val value: String) : CheckInWizardEvent
    data class OnThighLeftChanged(val value: String) : CheckInWizardEvent
    data class OnThighRightChanged(val value: String) : CheckInWizardEvent

    // Step 3
    data class OnToggleTag(val slug: String) : CheckInWizardEvent
    data class OnMentalFeelingChanged(val text: String) : CheckInWizardEvent
    data class OnPhysicalFeelingChanged(val text: String) : CheckInWizardEvent

    // Step 4
    data class OnAddPhoto(val imageData: ByteArray) : CheckInWizardEvent
    data class OnRemovePhoto(val photoId: String) : CheckInWizardEvent

    // Step 5
    data object OnSave : CheckInWizardEvent
}

sealed interface CheckInWizardEffect {
    data object NavigateBack : CheckInWizardEffect
    data class NavigateToDetail(val checkInId: String) : CheckInWizardEffect
    data class ShowError(val message: String) : CheckInWizardEffect
}
