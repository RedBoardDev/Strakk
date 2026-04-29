package com.strakk.shared.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.OnboardingData
import com.strakk.shared.domain.usecase.CreateProfileUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch

/**
 * Manages the 2-step onboarding flow (Goals Setup).
 *
 * Step 0: Protein + Calories goals.
 * Step 1: Water goal.
 *
 * On the last step's "Continue", builds [OnboardingData] from the form state,
 * calls [CreateProfileUseCase], and emits [OnboardingEffect.NavigateToHome] on success.
 */
class OnboardingViewModel(
    private val createProfile: CreateProfileUseCase,
) : MviViewModel<OnboardingUiState, OnboardingEvent, OnboardingEffect>(OnboardingUiState()) {

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.OnProteinGoalChanged ->
                setState { copy(proteinGoal = event.value) }
            is OnboardingEvent.OnCalorieGoalChanged ->
                setState { copy(calorieGoal = event.value) }
            is OnboardingEvent.OnWaterGoalChanged ->
                setState { copy(waterGoal = event.value) }
            is OnboardingEvent.OnContinue -> handleContinue()
            is OnboardingEvent.OnBack -> handleBack()
        }
    }

    private fun handleContinue() {
        val state = uiState.value
        if (state.isSaving) return

        if (state.isLastStep) {
            saveProfile(state)
        } else {
            setState { copy(currentStep = currentStep + 1) }
        }
    }

    private fun handleBack() {
        setState { if (isFirstStep) this else copy(currentStep = currentStep - 1) }
    }

    private fun saveProfile(state: OnboardingUiState) {
        setState { copy(isSaving = true) }

        viewModelScope.launch {
            createProfile(state.toOnboardingData())
                .onSuccess { emit(OnboardingEffect.NavigateToHome) }
                .onFailure { error ->
                    setState { copy(isSaving = false) }
                    emit(OnboardingEffect.ShowError(error.message ?: "Failed to save profile"))
                }
        }
    }

    private fun OnboardingUiState.toOnboardingData(): OnboardingData = OnboardingData(
        proteinGoal = proteinGoal.toIntOrNull(),
        calorieGoal = calorieGoal.toIntOrNull(),
        waterGoal = waterGoal.toIntOrNull(),
    )
}
