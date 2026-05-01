package com.strakk.android.ui.onboarding.activity

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
import com.strakk.shared.domain.model.DailyActivityLevel
import com.strakk.shared.domain.model.TrainingIntensity

@Composable
fun ActivityDailyContent(
    trainingIntensity: TrainingIntensity?,
    dailyActivityLevel: DailyActivityLevel?,
    onTrainingIntensityChanged: (TrainingIntensity?) -> Unit,
    onDailyActivityChanged: (DailyActivityLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_daily_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        // Training intensity section
        Text(
            text = stringResource(R.string.onboarding_daily_intensity_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SelectableCard(
                title = stringResource(R.string.onboarding_daily_intensity_light),
                subtitle = stringResource(R.string.onboarding_daily_intensity_light_desc),
                selected = trainingIntensity == TrainingIntensity.LIGHT,
                onClick = {
                    onTrainingIntensityChanged(
                        if (trainingIntensity == TrainingIntensity.LIGHT) null else TrainingIntensity.LIGHT,
                    )
                },
            )
            SelectableCard(
                title = stringResource(R.string.onboarding_daily_intensity_moderate),
                subtitle = stringResource(R.string.onboarding_daily_intensity_moderate_desc),
                selected = trainingIntensity == TrainingIntensity.MODERATE,
                onClick = {
                    onTrainingIntensityChanged(
                        if (trainingIntensity == TrainingIntensity.MODERATE) null else TrainingIntensity.MODERATE,
                    )
                },
            )
            SelectableCard(
                title = stringResource(R.string.onboarding_daily_intensity_intense),
                subtitle = stringResource(R.string.onboarding_daily_intensity_intense_desc),
                selected = trainingIntensity == TrainingIntensity.INTENSE,
                onClick = {
                    onTrainingIntensityChanged(
                        if (trainingIntensity == TrainingIntensity.INTENSE) null else TrainingIntensity.INTENSE,
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Daily activity section
        Text(
            text = stringResource(R.string.onboarding_daily_activity_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SelectableCard(
                title = stringResource(R.string.onboarding_daily_activity_sedentary),
                subtitle = stringResource(R.string.onboarding_daily_activity_sedentary_desc),
                selected = dailyActivityLevel == DailyActivityLevel.SEDENTARY,
                onClick = {
                    onDailyActivityChanged(
                        if (dailyActivityLevel == DailyActivityLevel.SEDENTARY) null else DailyActivityLevel.SEDENTARY,
                    )
                },
            )
            SelectableCard(
                title = stringResource(R.string.onboarding_daily_activity_moderate),
                subtitle = stringResource(R.string.onboarding_daily_activity_moderate_desc),
                selected = dailyActivityLevel == DailyActivityLevel.MODERATELY_ACTIVE,
                onClick = {
                    onDailyActivityChanged(
                        if (dailyActivityLevel == DailyActivityLevel.MODERATELY_ACTIVE) null else DailyActivityLevel.MODERATELY_ACTIVE,
                    )
                },
            )
            SelectableCard(
                title = stringResource(R.string.onboarding_daily_activity_very),
                subtitle = stringResource(R.string.onboarding_daily_activity_very_desc),
                selected = dailyActivityLevel == DailyActivityLevel.VERY_ACTIVE,
                onClick = {
                    onDailyActivityChanged(
                        if (dailyActivityLevel == DailyActivityLevel.VERY_ACTIVE) null else DailyActivityLevel.VERY_ACTIVE,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun ActivityDailyContentPreview() {
    StrakkTheme {
        ActivityDailyContent(
            trainingIntensity = TrainingIntensity.MODERATE,
            dailyActivityLevel = DailyActivityLevel.MODERATELY_ACTIVE,
            onTrainingIntensityChanged = {},
            onDailyActivityChanged = {},
        )
    }
}
