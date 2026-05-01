package com.strakk.shared.presentation.onboarding

import com.strakk.shared.domain.model.BiologicalSex
import com.strakk.shared.domain.model.DailyActivityLevel
import com.strakk.shared.domain.model.FitnessGoal
import com.strakk.shared.domain.model.NutritionDefaults
import com.strakk.shared.domain.model.TrainingIntensity
import com.strakk.shared.domain.model.TrainingType
import kotlinx.datetime.LocalDate

enum class OnboardingStep {
    WELCOME,
    WEIGHT,
    BIO,
    GOAL,
    ACTIVITY_TRAINING,
    ACTIVITY_DAILY,
    SIGN_UP,
    NUTRITION_GOALS,
    DAY_PREVIEW,
}

enum class AiCalculationState {
    AVAILABLE,
    LOADING,
    COMPLETED,
    FAILED,
}

data class OnboardingFlowUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,

    // Step 1: Weight
    val weightKg: Double = 75.0,

    // Step 2: Bio
    val heightCm: Int = 175,
    val heightSelected: Boolean = false,
    val birthDate: LocalDate? = null,
    val biologicalSex: BiologicalSex? = null,

    // Step 3: Goal
    val fitnessGoal: FitnessGoal? = null,

    // Step 4a: Activity — Training
    val trainingFrequency: Int? = null,
    val trainingTypes: Set<TrainingType> = emptySet(),

    // Step 4b: Activity — Daily
    val trainingIntensity: TrainingIntensity? = null,
    val dailyActivityLevel: DailyActivityLevel? = null,

    // Step 5: Sign Up
    val email: String = "",
    val password: String = "",
    val isSigningUp: Boolean = false,
    val signUpError: String? = null,

    // Step 6: Nutrition Goals
    val proteinGoal: Int = NutritionDefaults.PROTEIN_GOAL_GRAMS,
    val calorieGoal: Int = NutritionDefaults.CALORIE_GOAL_KCAL,
    val fatGoal: Int = 70,
    val carbGoal: Int = 250,
    val waterGoal: Int = NutritionDefaults.WATER_GOAL_ML,
    val aiState: AiCalculationState = AiCalculationState.AVAILABLE,

    // Global
    val isSaving: Boolean = false,
) {
    val progressFraction: Float
        get() = when (currentStep) {
            OnboardingStep.WELCOME -> 0f
            OnboardingStep.WEIGHT -> 0.2f
            OnboardingStep.BIO -> 0.4f
            OnboardingStep.GOAL -> 0.6f
            OnboardingStep.ACTIVITY_TRAINING,
            OnboardingStep.ACTIVITY_DAILY -> 0.8f
            OnboardingStep.SIGN_UP -> 1.0f
            OnboardingStep.NUTRITION_GOALS,
            OnboardingStep.DAY_PREVIEW -> 1.0f
        }

    val showProgressBar: Boolean
        get() = currentStep.ordinal in OnboardingStep.WEIGHT.ordinal..OnboardingStep.SIGN_UP.ordinal

    val showBackButton: Boolean
        get() = currentStep != OnboardingStep.WELCOME &&
            currentStep != OnboardingStep.NUTRITION_GOALS &&
            currentStep != OnboardingStep.DAY_PREVIEW
}

sealed interface OnboardingFlowEvent {
    // Navigation
    data object OnContinue : OnboardingFlowEvent
    data object OnBack : OnboardingFlowEvent
    data object OnNavigateToLogin : OnboardingFlowEvent

    // Step 1: Weight
    data class OnWeightChanged(val kg: Double) : OnboardingFlowEvent

    // Step 2: Bio
    data class OnHeightChanged(val cm: Int) : OnboardingFlowEvent
    data class OnBirthDateChanged(val date: LocalDate?) : OnboardingFlowEvent
    data class OnBiologicalSexChanged(val sex: BiologicalSex?) : OnboardingFlowEvent

    // Step 3: Goal
    data class OnFitnessGoalChanged(val goal: FitnessGoal?) : OnboardingFlowEvent

    // Step 4a: Activity — Training
    data class OnTrainingFrequencyChanged(val frequency: Int?) : OnboardingFlowEvent
    data class OnTrainingTypeToggled(val type: TrainingType) : OnboardingFlowEvent

    // Step 4b: Activity — Daily
    data class OnTrainingIntensityChanged(val intensity: TrainingIntensity?) : OnboardingFlowEvent
    data class OnDailyActivityChanged(val level: DailyActivityLevel?) : OnboardingFlowEvent

    // Step 5: Sign Up
    data class OnEmailChanged(val email: String) : OnboardingFlowEvent
    data class OnPasswordChanged(val password: String) : OnboardingFlowEvent

    // Step 6: Nutrition Goals
    data object OnCalculateWithAi : OnboardingFlowEvent
    data class OnProteinGoalChanged(val value: Int) : OnboardingFlowEvent
    data class OnCalorieGoalChanged(val value: Int) : OnboardingFlowEvent
    data class OnFatGoalChanged(val value: Int) : OnboardingFlowEvent
    data class OnCarbGoalChanged(val value: Int) : OnboardingFlowEvent
    data class OnWaterGoalChanged(val value: Int) : OnboardingFlowEvent
}

sealed interface OnboardingFlowEffect {
    data object NavigateToHome : OnboardingFlowEffect
    data object NavigateToLogin : OnboardingFlowEffect
    data class ShowError(val message: String) : OnboardingFlowEffect
}
