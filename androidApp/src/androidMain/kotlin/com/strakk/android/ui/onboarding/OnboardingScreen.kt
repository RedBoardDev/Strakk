package com.strakk.android.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.onboarding.OnboardingEvent
import com.strakk.shared.presentation.onboarding.OnboardingUiState

private const val TOTAL_STEPS = 3

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 120.dp),
        ) {
            // Step dots indicator
            StepIndicator(
                currentStep = uiState.currentStep,
                totalSteps = TOTAL_STEPS,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Animated step content — horizontal slide
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    slideInHorizontally { width -> direction * width } togetherWith
                        slideOutHorizontally { width -> -direction * width }
                },
                label = "onboarding_step",
            ) { step ->
                when (step) {
                    0 -> GoalsStepContent(
                        proteinGoal = uiState.proteinGoal,
                        calorieGoal = uiState.calorieGoal,
                        onProteinGoalChanged = { onEvent(OnboardingEvent.OnProteinGoalChanged(it)) },
                        onCalorieGoalChanged = { onEvent(OnboardingEvent.OnCalorieGoalChanged(it)) },
                    )
                    1 -> WaterStepContent(
                        waterGoal = uiState.waterGoal,
                        onWaterGoalChanged = { onEvent(OnboardingEvent.OnWaterGoalChanged(it)) },
                    )
                    2 -> RemindersStepContent(
                        trackingReminderEnabled = uiState.trackingReminderEnabled,
                        trackingReminderTime = uiState.trackingReminderTime,
                        checkinReminderEnabled = uiState.checkinReminderEnabled,
                        checkinReminderDay = uiState.checkinReminderDay,
                        checkinReminderTime = uiState.checkinReminderTime,
                        onTrackingReminderToggled = { onEvent(OnboardingEvent.OnTrackingReminderToggled(it)) },
                        onTrackingReminderTimeChanged = { onEvent(OnboardingEvent.OnTrackingReminderTimeChanged(it)) },
                        onCheckinReminderToggled = { onEvent(OnboardingEvent.OnCheckinReminderToggled(it)) },
                        onCheckinReminderDayChanged = { onEvent(OnboardingEvent.OnCheckinReminderDayChanged(it)) },
                        onCheckinReminderTimeChanged = { onEvent(OnboardingEvent.OnCheckinReminderTimeChanged(it)) },
                    )
                    else -> Unit
                }
            }
        }

        // Bottom action buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Button(
                onClick = { onEvent(OnboardingEvent.OnContinue) },
                enabled = !uiState.isSaving,
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
                    text = if (uiState.isLastStep) "Get started" else "Continue",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (!uiState.isFirstStep) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { onEvent(OnboardingEvent.OnBack) }) {
                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalStrakkColors.current.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (index == currentStep) MaterialTheme.colorScheme.primary else LocalStrakkColors.current.surface2),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun OnboardingScreenStep0Preview() {
    StrakkTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(currentStep = 0),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun OnboardingScreenStep2Preview() {
    StrakkTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(currentStep = 2),
            onEvent = {},
        )
    }
}
