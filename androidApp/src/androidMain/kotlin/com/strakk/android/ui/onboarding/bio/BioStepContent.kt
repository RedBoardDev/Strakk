package com.strakk.android.ui.onboarding.bio

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.onboarding.components.StepperRow
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.BiologicalSex
import kotlinx.datetime.LocalDate
import java.util.Calendar

@Composable
fun BioStepContent(
    heightCm: Int,
    heightSelected: Boolean,
    birthDate: LocalDate?,
    biologicalSex: BiologicalSex?,
    onHeightChanged: (Int) -> Unit,
    onBirthDateChanged: (LocalDate?) -> Unit,
    onBiologicalSexChanged: (BiologicalSex?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_bio_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        Text(
            text = stringResource(R.string.onboarding_bio_optional),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = spacing.xs),
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        // Height
        Text(
            text = stringResource(R.string.onboarding_bio_height_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        StepperRow(
            label = stringResource(R.string.onboarding_bio_height_label),
            value = heightCm,
            unit = stringResource(R.string.onboarding_bio_height_unit),
            onDecrement = { onHeightChanged((heightCm - 1).coerceAtLeast(100)) },
            onIncrement = { onHeightChanged((heightCm + 1).coerceAtMost(230)) },
            minValue = 100,
            maxValue = 230,
        )

        Spacer(modifier = Modifier.height(spacing.xl))

        // Birth date
        Text(
            text = stringResource(R.string.onboarding_bio_birth_date_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        OutlinedButton(
            onClick = {
                val calendar = Calendar.getInstance()
                birthDate?.let {
                    calendar.set(it.year, it.monthNumber - 1, it.dayOfMonth)
                }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onBirthDateChanged(LocalDate(year, month + 1, day))
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                ).apply {
                    datePicker.maxDate = Calendar.getInstance().timeInMillis
                }.show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (birthDate != null) "${birthDate.dayOfMonth}/${birthDate.monthNumber}/${birthDate.year}"
                    else stringResource(R.string.onboarding_bio_birth_date_placeholder),
                color = if (birthDate != null) colors.textPrimary else colors.textTertiary,
            )
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Sex
        Text(
            text = stringResource(R.string.onboarding_bio_sex_label),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            modifier = Modifier.fillMaxWidth(),
        ) {
            BiologicalSexChip(
                label = stringResource(R.string.onboarding_bio_sex_male),
                selected = biologicalSex == BiologicalSex.MALE,
                onClick = {
                    onBiologicalSexChanged(
                        if (biologicalSex == BiologicalSex.MALE) null else BiologicalSex.MALE,
                    )
                },
                modifier = Modifier.weight(1f),
            )
            BiologicalSexChip(
                label = stringResource(R.string.onboarding_bio_sex_female),
                selected = biologicalSex == BiologicalSex.FEMALE,
                onClick = {
                    onBiologicalSexChanged(
                        if (biologicalSex == BiologicalSex.FEMALE) null else BiologicalSex.FEMALE,
                    )
                },
                modifier = Modifier.weight(1f),
            )
            BiologicalSexChip(
                label = stringResource(R.string.onboarding_bio_sex_unspecified),
                selected = biologicalSex == BiologicalSex.UNSPECIFIED,
                onClick = {
                    onBiologicalSexChanged(
                        if (biologicalSex == BiologicalSex.UNSPECIFIED) null else BiologicalSex.UNSPECIFIED,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BiologicalSexChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = colors.surface2,
            labelColor = colors.textSecondary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderColor = colors.borderFaint,
        ),
        modifier = modifier,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun BioStepContentPreview() {
    StrakkTheme {
        BioStepContent(
            heightCm = 175,
            heightSelected = true,
            birthDate = null,
            biologicalSex = BiologicalSex.MALE,
            onHeightChanged = {},
            onBirthDateChanged = {},
            onBiologicalSexChanged = {},
        )
    }
}
