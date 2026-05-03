package com.strakk.android.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.onboarding.activity.ActivityDailyContent
import com.strakk.android.ui.onboarding.activity.ActivityTrainingContent
import com.strakk.android.ui.onboarding.bio.BioStepContent
import com.strakk.android.ui.onboarding.components.OnboardingProgressBar
import com.strakk.android.ui.onboarding.goal.GoalStepContent
import com.strakk.android.ui.onboarding.goals.NutritionGoalsContent
import com.strakk.android.ui.onboarding.preview.DayPreviewContent
import com.strakk.android.ui.onboarding.prooffer.ProOfferContent
import com.strakk.android.ui.onboarding.signup.SignUpStepContent
import com.strakk.android.ui.onboarding.weight.WeightStepContent
import com.strakk.android.ui.onboarding.welcome.WelcomeContent
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.onboarding.OnboardingFlowEvent
import com.strakk.shared.presentation.onboarding.OnboardingFlowUiState
import com.strakk.shared.presentation.onboarding.OnboardingStep

@Composable
fun OnboardingFlowScreen(
    uiState: OnboardingFlowUiState,
    onEvent: (OnboardingFlowEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalStrakkSpacing.current
    val currentStepIndex = uiState.currentStep.ordinal

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Progress bar — visible for steps WEIGHT through SIGN_UP
            if (uiState.showProgressBar) {
                Spacer(modifier = Modifier.height(spacing.md))
                OnboardingProgressBar(
                    progress = uiState.progressFraction,
                    modifier = Modifier.padding(horizontal = spacing.xl),
                )
            }

            // Step content — scrollable, with animated horizontal transitions
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    slideInHorizontally { width -> direction * width } togetherWith
                        slideOutHorizontally { width -> -direction * width }
                },
                label = "onboarding_flow_step",
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.xl)
                    .padding(top = spacing.xxl, bottom = spacing.xl),
            ) { step ->
                OnboardingStepContent(
                    step = step,
                    uiState = uiState,
                    onEvent = onEvent,
                    onNavigateToLogin = onNavigateToLogin,
                )
            }

            // Bottom action bar — hidden for WELCOME (it has its own CTA) and SIGN_UP
            val showBottomBar = uiState.currentStep != OnboardingStep.WELCOME &&
                uiState.currentStep != OnboardingStep.SIGN_UP &&
                uiState.currentStep != OnboardingStep.PRO_OFFER
            if (showBottomBar) {
                BottomActionBar(
                    currentStep = uiState.currentStep,
                    isSaving = uiState.isSaving,
                    showBackButton = uiState.showBackButton,
                    onContinue = { onEvent(OnboardingFlowEvent.OnContinue) },
                    onBack = { onEvent(OnboardingFlowEvent.OnBack) },
                )
            }
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun OnboardingStepContent(
    step: OnboardingStep,
    uiState: OnboardingFlowUiState,
    onEvent: (OnboardingFlowEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    when (step) {
        OnboardingStep.WELCOME -> WelcomeContent(
            onContinue = { onEvent(OnboardingFlowEvent.OnContinue) },
            onNavigateToLogin = onNavigateToLogin,
        )
        OnboardingStep.WEIGHT -> WeightStepContent(
            weightKg = uiState.weightKg,
            onWeightChanged = { onEvent(OnboardingFlowEvent.OnWeightChanged(it)) },
        )
        OnboardingStep.BIO -> BioStepContent(
            heightCm = uiState.heightCm,
            heightSelected = uiState.heightSelected,
            birthDate = uiState.birthDate,
            biologicalSex = uiState.biologicalSex,
            onHeightChanged = { onEvent(OnboardingFlowEvent.OnHeightChanged(it)) },
            onBirthDateChanged = { onEvent(OnboardingFlowEvent.OnBirthDateChanged(it)) },
            onBiologicalSexChanged = { onEvent(OnboardingFlowEvent.OnBiologicalSexChanged(it)) },
        )
        OnboardingStep.GOAL -> GoalStepContent(
            fitnessGoal = uiState.fitnessGoal,
            onFitnessGoalChanged = { onEvent(OnboardingFlowEvent.OnFitnessGoalChanged(it)) },
        )
        OnboardingStep.ACTIVITY_TRAINING -> ActivityTrainingContent(
            trainingFrequency = uiState.trainingFrequency,
            trainingTypes = uiState.trainingTypes,
            onTrainingFrequencyChanged = { onEvent(OnboardingFlowEvent.OnTrainingFrequencyChanged(it)) },
            onTrainingTypeToggled = { onEvent(OnboardingFlowEvent.OnTrainingTypeToggled(it)) },
        )
        OnboardingStep.ACTIVITY_DAILY -> ActivityDailyContent(
            trainingIntensity = uiState.trainingIntensity,
            dailyActivityLevel = uiState.dailyActivityLevel,
            onTrainingIntensityChanged = { onEvent(OnboardingFlowEvent.OnTrainingIntensityChanged(it)) },
            onDailyActivityChanged = { onEvent(OnboardingFlowEvent.OnDailyActivityChanged(it)) },
        )
        OnboardingStep.SIGN_UP -> SignUpStepContent(
            email = uiState.email,
            password = uiState.password,
            isLoading = uiState.isSigningUp,
            error = uiState.signUpError,
            onEmailChanged = { onEvent(OnboardingFlowEvent.OnEmailChanged(it)) },
            onPasswordChanged = { onEvent(OnboardingFlowEvent.OnPasswordChanged(it)) },
            onSignUp = { onEvent(OnboardingFlowEvent.OnContinue) },
            onNavigateToLogin = onNavigateToLogin,
        )
        OnboardingStep.NUTRITION_GOALS -> NutritionGoalsContent(
            proteinGoal = uiState.proteinGoal,
            calorieGoal = uiState.calorieGoal,
            fatGoal = uiState.fatGoal,
            carbGoal = uiState.carbGoal,
            waterGoal = uiState.waterGoal,
            aiState = uiState.aiState,
            onCalculateWithAi = { onEvent(OnboardingFlowEvent.OnCalculateWithAi) },
            onProteinGoalChanged = { onEvent(OnboardingFlowEvent.OnProteinGoalChanged(it)) },
            onCalorieGoalChanged = { onEvent(OnboardingFlowEvent.OnCalorieGoalChanged(it)) },
            onFatGoalChanged = { onEvent(OnboardingFlowEvent.OnFatGoalChanged(it)) },
            onCarbGoalChanged = { onEvent(OnboardingFlowEvent.OnCarbGoalChanged(it)) },
            onWaterGoalChanged = { onEvent(OnboardingFlowEvent.OnWaterGoalChanged(it)) },
        )
        OnboardingStep.DAY_PREVIEW -> DayPreviewContent(
            proteinGoal = uiState.proteinGoal,
            calorieGoal = uiState.calorieGoal,
            fatGoal = uiState.fatGoal,
            carbGoal = uiState.carbGoal,
            waterGoal = uiState.waterGoal,
        )
        OnboardingStep.PRO_OFFER -> ProOfferContent(
            onStartFreeTrial = { onEvent(OnboardingFlowEvent.OnStartFreeTrial) },
        )
    }
}

@Composable
private fun BottomActionBar(
    currentStep: OnboardingStep,
    isSaving: Boolean,
    showBackButton: Boolean,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalStrakkSpacing.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.xl)
            .padding(bottom = spacing.xxl),
    ) {
        Button(
            onClick = onContinue,
            enabled = !isSaving,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = when (currentStep) {
                    OnboardingStep.DAY_PREVIEW -> stringResource(R.string.onboarding_preview_cta)
                    else -> stringResource(R.string.onboarding_continue)
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }

        if (showBackButton) {
            Spacer(modifier = Modifier.height(spacing.xxs))
            TextButton(onClick = onBack) {
                Text(
                    text = stringResource(R.string.onboarding_back),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun OnboardingFlowScreenWelcomePreview() {
    StrakkTheme {
        OnboardingFlowScreen(
            uiState = OnboardingFlowUiState(currentStep = OnboardingStep.WELCOME),
            onEvent = {},
            onNavigateToLogin = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun OnboardingFlowScreenWeightPreview() {
    StrakkTheme {
        OnboardingFlowScreen(
            uiState = OnboardingFlowUiState(currentStep = OnboardingStep.WEIGHT),
            onEvent = {},
            onNavigateToLogin = {},
        )
    }
}
