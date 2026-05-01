package com.strakk.android.ui.onboarding.activity

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
import com.strakk.android.ui.onboarding.components.ChipGrid
import com.strakk.android.ui.onboarding.components.PillSelector
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.TrainingType

private val TRAINING_TYPES = listOf(
    TrainingType.STRENGTH,
    TrainingType.CARDIO,
    TrainingType.TEAM_SPORT,
    TrainingType.YOGA_FLEXIBILITY,
    TrainingType.OTHER,
)

@Composable
fun ActivityTrainingContent(
    trainingFrequency: Int?,
    trainingTypes: Set<TrainingType>,
    onTrainingFrequencyChanged: (Int?) -> Unit,
    onTrainingTypeToggled: (TrainingType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    // Labels for frequency pills: 0-7
    val frequencyLabels = (0..7).map { it.toString() }

    // Labels for training type chips — ordered by TRAINING_TYPES list
    val typeLabels = listOf(
        stringResource(R.string.onboarding_training_type_strength),
        stringResource(R.string.onboarding_training_type_cardio),
        stringResource(R.string.onboarding_training_type_team),
        stringResource(R.string.onboarding_training_type_yoga),
        stringResource(R.string.onboarding_training_type_other),
    )

    val selectedTypeIndices = trainingTypes.mapNotNull { type ->
        TRAINING_TYPES.indexOf(type).takeIf { it >= 0 }
    }.toSet()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_training_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        Text(
            text = stringResource(R.string.onboarding_training_frequency_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        PillSelector(
            items = frequencyLabels,
            selectedIndex = trainingFrequency,
            onSelect = { index ->
                onTrainingFrequencyChanged(if (trainingFrequency == index) null else index)
            },
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        Text(
            text = stringResource(R.string.onboarding_training_types_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        ChipGrid(
            items = typeLabels,
            selectedIndices = selectedTypeIndices,
            onToggle = { index ->
                if (index in TRAINING_TYPES.indices) {
                    onTrainingTypeToggled(TRAINING_TYPES[index])
                }
            },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun ActivityTrainingContentPreview() {
    StrakkTheme {
        ActivityTrainingContent(
            trainingFrequency = 3,
            trainingTypes = setOf(TrainingType.STRENGTH, TrainingType.CARDIO),
            onTrainingFrequencyChanged = {},
            onTrainingTypeToggled = {},
        )
    }
}
