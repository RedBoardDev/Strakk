package com.strakk.android.ui.onboarding.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.strakk.android.R
import com.strakk.android.ui.onboarding.components.SelectableCard
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.FitnessGoal

@Composable
fun GoalStepContent(
    fitnessGoal: FitnessGoal?,
    onFitnessGoalChanged: (FitnessGoal?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_goal_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            GoalEntry.entries.forEach { entry ->
                SelectableCard(
                    title = stringResource(entry.titleRes),
                    subtitle = stringResource(entry.subtitleRes),
                    selected = fitnessGoal == entry.goal,
                    onClick = {
                        onFitnessGoalChanged(if (fitnessGoal == entry.goal) null else entry.goal)
                    },
                )
            }
        }
    }
}

private enum class GoalEntry(
    val goal: FitnessGoal,
    val titleRes: Int,
    val subtitleRes: Int,
) {
    LOSE_FAT(FitnessGoal.LOSE_FAT, R.string.onboarding_goal_lose_fat, R.string.onboarding_goal_lose_fat_desc),
    GAIN_MUSCLE(FitnessGoal.GAIN_MUSCLE, R.string.onboarding_goal_gain_muscle, R.string.onboarding_goal_gain_muscle_desc),
    MAINTAIN(FitnessGoal.MAINTAIN, R.string.onboarding_goal_maintain, R.string.onboarding_goal_maintain_desc),
    JUST_TRACK(FitnessGoal.JUST_TRACK, R.string.onboarding_goal_just_track, R.string.onboarding_goal_just_track_desc),
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun GoalStepContentPreview() {
    StrakkTheme {
        GoalStepContent(
            fitnessGoal = FitnessGoal.LOSE_FAT,
            onFitnessGoalChanged = {},
        )
    }
}
