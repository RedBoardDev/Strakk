package com.strakk.shared.presentation.onboarding

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.CalculateGoalsRequest
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.OnboardingData
import com.strakk.shared.domain.usecase.CalculateGoalsUseCase
import com.strakk.shared.domain.usecase.CompleteOnboardingUseCase
import com.strakk.shared.domain.usecase.CreateProfileUseCase
import com.strakk.shared.domain.usecase.SignUpUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private const val AI_TIMEOUT_MS = 15_000L
private const val MIN_PASSWORD_LENGTH = 6

class OnboardingFlowViewModel(
    private val signUp: SignUpUseCase,
    private val createProfile: CreateProfileUseCase,
    private val calculateGoals: CalculateGoalsUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
) : MviViewModel<OnboardingFlowUiState, OnboardingFlowEvent, OnboardingFlowEffect>(
    OnboardingFlowUiState(),
) {

    @Suppress("CyclomaticComplexMethod")
    override fun onEvent(event: OnboardingFlowEvent) {
        when (event) {
            is OnboardingFlowEvent.OnContinue -> handleContinue()
            is OnboardingFlowEvent.OnBack -> handleBack()
            is OnboardingFlowEvent.OnNavigateToLogin -> emit(OnboardingFlowEffect.NavigateToLogin)

            is OnboardingFlowEvent.OnWeightChanged -> setState { copy(weightKg = event.kg) }
            is OnboardingFlowEvent.OnHeightChanged -> setState { copy(heightCm = event.cm, heightSelected = true) }
            is OnboardingFlowEvent.OnBirthDateChanged -> setState { copy(birthDate = event.date) }
            is OnboardingFlowEvent.OnBiologicalSexChanged -> setState { copy(biologicalSex = event.sex) }

            is OnboardingFlowEvent.OnFitnessGoalChanged -> setState { copy(fitnessGoal = event.goal) }

            is OnboardingFlowEvent.OnTrainingFrequencyChanged -> setState { copy(trainingFrequency = event.frequency) }
            is OnboardingFlowEvent.OnTrainingTypeToggled -> setState {
                val updated = if (event.type in trainingTypes) trainingTypes - event.type else trainingTypes + event.type
                copy(trainingTypes = updated)
            }

            is OnboardingFlowEvent.OnTrainingIntensityChanged -> setState { copy(trainingIntensity = event.intensity) }
            is OnboardingFlowEvent.OnDailyActivityChanged -> setState { copy(dailyActivityLevel = event.level) }

            is OnboardingFlowEvent.OnEmailChanged -> setState { copy(email = event.email, signUpError = null) }
            is OnboardingFlowEvent.OnPasswordChanged -> setState { copy(password = event.password, signUpError = null) }

            is OnboardingFlowEvent.OnCalculateWithAi -> handleCalculateWithAi()
            is OnboardingFlowEvent.OnProteinGoalChanged -> setState { copy(proteinGoal = event.value) }
            is OnboardingFlowEvent.OnCalorieGoalChanged -> setState { copy(calorieGoal = event.value) }
            is OnboardingFlowEvent.OnFatGoalChanged -> setState { copy(fatGoal = event.value) }
            is OnboardingFlowEvent.OnCarbGoalChanged -> setState { copy(carbGoal = event.value) }
            is OnboardingFlowEvent.OnWaterGoalChanged -> setState { copy(waterGoal = event.value) }
        }
    }

    private fun handleContinue() {
        val state = uiState.value
        if (state.isSigningUp || state.isSaving) return

        when (state.currentStep) {
            OnboardingStep.SIGN_UP -> handleSignUp(state)
            OnboardingStep.DAY_PREVIEW -> handleComplete(state)
            else -> advanceStep(state.currentStep)
        }
    }

    private fun handleBack() {
        val state = uiState.value
        val previousStep = when (state.currentStep) {
            OnboardingStep.WELCOME -> return
            OnboardingStep.WEIGHT -> OnboardingStep.WELCOME
            OnboardingStep.BIO -> OnboardingStep.WEIGHT
            OnboardingStep.GOAL -> OnboardingStep.BIO
            OnboardingStep.ACTIVITY_TRAINING -> OnboardingStep.GOAL
            OnboardingStep.ACTIVITY_DAILY -> OnboardingStep.ACTIVITY_TRAINING
            OnboardingStep.SIGN_UP -> OnboardingStep.ACTIVITY_DAILY
            OnboardingStep.NUTRITION_GOALS -> return
            OnboardingStep.DAY_PREVIEW -> OnboardingStep.NUTRITION_GOALS
        }
        setState { copy(currentStep = previousStep) }
    }

    private fun advanceStep(current: OnboardingStep) {
        val nextStep = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.WEIGHT
            OnboardingStep.WEIGHT -> OnboardingStep.BIO
            OnboardingStep.BIO -> OnboardingStep.GOAL
            OnboardingStep.GOAL -> OnboardingStep.ACTIVITY_TRAINING
            OnboardingStep.ACTIVITY_TRAINING -> OnboardingStep.ACTIVITY_DAILY
            OnboardingStep.ACTIVITY_DAILY -> OnboardingStep.SIGN_UP
            OnboardingStep.SIGN_UP -> OnboardingStep.NUTRITION_GOALS
            OnboardingStep.NUTRITION_GOALS -> OnboardingStep.DAY_PREVIEW
            OnboardingStep.DAY_PREVIEW -> return
        }
        setState { copy(currentStep = nextStep) }
    }

    private fun handleSignUp(state: OnboardingFlowUiState) {
        val trimmedEmail = state.email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            setState { copy(signUpError = "Email invalide.") }
            return
        }
        if (state.password.length < MIN_PASSWORD_LENGTH) {
            setState { copy(signUpError = "Le mot de passe doit contenir au moins $MIN_PASSWORD_LENGTH caractères.") }
            return
        }

        setState { copy(isSigningUp = true, signUpError = null) }

        viewModelScope.launch {
            signUp(trimmedEmail, state.password)
                .onSuccess {
                    createProfile(state.toOnboardingData())
                        .onSuccess {
                            setState { copy(isSigningUp = false, currentStep = OnboardingStep.NUTRITION_GOALS) }
                        }
                        .onFailure { error ->
                            setState { copy(isSigningUp = false) }
                            emit(OnboardingFlowEffect.ShowError(error.message ?: "Erreur lors de la création du profil."))
                        }
                }
                .onFailure { error ->
                    setState {
                        copy(
                            isSigningUp = false,
                            signUpError = mapSignUpError(error),
                        )
                    }
                }
        }
    }

    private fun handleCalculateWithAi() {
        val state = uiState.value
        if (state.aiState != AiCalculationState.AVAILABLE) return

        setState { copy(aiState = AiCalculationState.LOADING) }

        viewModelScope.launch {
            val request = state.toCalculateGoalsRequest()
            val result = withTimeoutOrNull(AI_TIMEOUT_MS) {
                calculateGoals(request)
            }

            when {
                result == null -> setState { copy(aiState = AiCalculationState.FAILED) }
                result.isSuccess -> {
                    val goals = result.getOrThrow()
                    setState {
                        copy(
                            proteinGoal = goals.proteinG,
                            calorieGoal = goals.caloriesKcal,
                            fatGoal = goals.fatG,
                            carbGoal = goals.carbsG,
                            waterGoal = goals.waterMl,
                            aiState = AiCalculationState.COMPLETED,
                        )
                    }
                }
                else -> setState { copy(aiState = AiCalculationState.FAILED) }
            }
        }
    }

    private fun handleComplete(state: OnboardingFlowUiState) {
        setState { copy(isSaving = true) }

        viewModelScope.launch {
            val goals = NutritionGoals(
                proteinGoal = state.proteinGoal,
                calorieGoal = state.calorieGoal,
                fatGoal = state.fatGoal,
                carbGoal = state.carbGoal,
                waterGoal = state.waterGoal,
            )

            completeOnboarding(goals)
                .onSuccess { emit(OnboardingFlowEffect.NavigateToHome) }
                .onFailure { error ->
                    setState { copy(isSaving = false) }
                    emit(OnboardingFlowEffect.ShowError(error.message ?: "Erreur lors de la sauvegarde."))
                }
        }
    }

    private fun OnboardingFlowUiState.toOnboardingData(): OnboardingData = OnboardingData(
        weightKg = weightKg,
        heightCm = if (heightSelected) heightCm else null,
        birthDate = birthDate,
        biologicalSex = biologicalSex,
        fitnessGoal = fitnessGoal,
        trainingFrequency = trainingFrequency,
        trainingTypes = trainingTypes,
        trainingIntensity = trainingIntensity,
        dailyActivityLevel = dailyActivityLevel,
        proteinGoal = null,
        calorieGoal = null,
        fatGoal = null,
        carbGoal = null,
        waterGoal = null,
    )

    private fun OnboardingFlowUiState.toCalculateGoalsRequest(): CalculateGoalsRequest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val age = birthDate?.let {
            val years = today.year - it.year
            if (today.monthNumber < it.monthNumber ||
                (today.monthNumber == it.monthNumber && today.dayOfMonth < it.dayOfMonth)
            ) years - 1 else years
        }

        return CalculateGoalsRequest(
            weightKg = weightKg,
            heightCm = if (heightSelected) heightCm else null,
            age = age,
            biologicalSex = biologicalSex,
            fitnessGoal = fitnessGoal,
            trainingFrequencyPerWeek = trainingFrequency,
            trainingTypes = trainingTypes,
            trainingIntensity = trainingIntensity,
            dailyActivityLevel = dailyActivityLevel,
        )
    }

    private fun mapSignUpError(error: Throwable): String {
        val message = error.message?.lowercase() ?: return "Une erreur est survenue. Réessaie."
        return when {
            "already registered" in message || "already exists" in message ->
                "Un compte existe déjà avec cet email."
            "rate" in message || "too many" in message ->
                "Trop de tentatives. Réessaie dans quelques minutes."
            "network" in message || "connect" in message ->
                "Pas de connexion. Vérifie ton réseau."
            else -> "Une erreur est survenue. Réessaie."
        }
    }
}
