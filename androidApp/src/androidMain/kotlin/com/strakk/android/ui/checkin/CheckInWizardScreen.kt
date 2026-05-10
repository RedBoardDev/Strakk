package com.strakk.android.ui.checkin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.StrakkLoadingState
import com.strakk.android.ui.components.StrakkPrimaryButton
import com.strakk.android.ui.components.StrakkSecondaryButton
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.checkin.CheckInWizardEvent
import com.strakk.shared.presentation.checkin.CheckInWizardUiState
import com.strakk.shared.presentation.checkin.DayOption
import com.strakk.shared.presentation.checkin.WeekOption
import com.strakk.shared.presentation.checkin.WizardPhoto
import com.strakk.shared.presentation.checkin.WizardStep

private const val MAX_PHOTOS = 3
private const val PHOTO_THUMBNAIL_HEIGHT_DP = 100

// Predefined feeling tags. Slugs match what the VM expects.
private val feelingTagOptions = listOf(
    "energy" to R.string.checkin_tag_energy,
    "fatigue" to R.string.checkin_tag_fatigue,
    "stress" to R.string.checkin_tag_stress,
    "sleep" to R.string.checkin_tag_sleep,
    "motivation" to R.string.checkin_tag_motivation,
    "soreness" to R.string.checkin_tag_soreness,
    "digestion" to R.string.checkin_tag_digestion,
    "mood" to R.string.checkin_tag_mood,
)

@Composable
fun CheckInWizardScreen(
    state: CheckInWizardUiState,
    onEvent: (CheckInWizardEvent) -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it) } },
    ) { innerPadding ->
        when (state) {
            CheckInWizardUiState.Loading -> StrakkLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is CheckInWizardUiState.Ready -> ReadyWizard(
                state = state,
                onEvent = onEvent,
                padding = innerPadding,
            )
        }
    }
}

// =============================================================================
// Main wizard layout
// =============================================================================

@Suppress("LongMethod")
@Composable
private fun ReadyWizard(
    state: CheckInWizardUiState.Ready,
    onEvent: (CheckInWizardEvent) -> Unit,
    padding: PaddingValues,
) {
    val spacing = LocalStrakkSpacing.current
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    val stepIndex = WizardStep.entries.indexOf(state.currentStep)
    val totalSteps = WizardStep.entries.size
    val progress = (stepIndex + 1).toFloat() / totalSteps

    val titleRes = if (state.isEditMode) R.string.checkin_wizard_title_edit else R.string.checkin_wizard_title_new

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = textStyles.bodyBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.IconButton(
                    onClick = { onEvent(CheckInWizardEvent.OnCancel) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_cancel),
                        tint = colors.textSecondary,
                    )
                }
            }
            Text(
                text = wizardStepLabel(state.currentStep),
                style = textStyles.caption,
                color = colors.textTertiary,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = colors.accentOrange,
                trackColor = colors.surface2,
            )
        }

        // Step content — scrollable
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            when (state.currentStep) {
                WizardStep.Dates -> DatesStep(state = state, onEvent = onEvent)
                WizardStep.Measurements -> MeasurementsStep(state = state, onEvent = onEvent)
                WizardStep.Feelings -> FeelingsStep(state = state, onEvent = onEvent)
                WizardStep.Photos -> PhotosStep(state = state, onEvent = onEvent)
                WizardStep.Summary -> SummaryStep(state = state, onEvent = onEvent)
            }
            Spacer(Modifier.height(spacing.sm))
        }

        // Bottom nav bar
        WizardBottomBar(state = state, onEvent = onEvent)
    }
}

// =============================================================================
// Step 1 — Dates
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun DatesStep(
    state: CheckInWizardUiState.Ready,
    onEvent: (CheckInWizardEvent) -> Unit,
) {
    val spacing = LocalStrakkSpacing.current

    SectionLabel(R.string.checkin_wizard_week_selector)
    WeekDropdown(
        weeks = state.availableWeeks,
        selectedWeek = state.weekLabel,
        isEditMode = state.isEditMode,
        onSelect = { onEvent(CheckInWizardEvent.OnSelectWeek(it)) },
    )

    if (state.existingCheckInId != null) {
        Spacer(Modifier.height(spacing.xs))
        ExistingWarningRow()
    }

    Spacer(Modifier.height(spacing.xs))
    SectionLabel(R.string.checkin_wizard_days_selected)
    DayToggleRow(
        days = state.weekDays,
        onToggle = { onEvent(CheckInWizardEvent.OnToggleDate(it)) },
    )
}

@Suppress("FunctionSignature")
@Composable
private fun WeekDropdown(
    weeks: List<WeekOption>,
    selectedWeek: String,
    isEditMode: Boolean,
    onSelect: (String) -> Unit,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val selected = weeks.find { it.weekLabel == selectedWeek }
    val expanded = remember { androidx.compose.runtime.mutableStateOf(false) }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface1)
                .then(if (!isEditMode) Modifier.clickable { expanded.value = true } else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = selected?.displayLabel ?: selectedWeek,
                style = textStyles.bodyBold,
                color = if (isEditMode) colors.textTertiary else colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${selected?.startDate} → ${selected?.endDate}",
                style = textStyles.caption,
                color = colors.textTertiary,
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            weeks.forEach { week ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Column {
                            Text(week.displayLabel, style = textStyles.bodyBold)
                            Text("${week.startDate} → ${week.endDate}", style = textStyles.caption)
                        }
                    },
                    onClick = {
                        expanded.value = false
                        onSelect(week.weekLabel)
                    },
                )
            }
        }
    }
}

@Composable
private fun ExistingWarningRow() {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.warning.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Outlined.Warning, contentDescription = null, tint = colors.warning, modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.checkin_wizard_existing_warning),
            style = textStyles.caption,
            color = colors.warning,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("FunctionSignature")
@Composable
private fun DayToggleRow(
    days: List<DayOption>,
    onToggle: (String) -> Unit,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        days.forEach { day ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (day.selected) colors.accentOrange else colors.surface2)
                    .border(
                        width = 1.dp,
                        color = if (day.selected) colors.accentOrange else colors.divider,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onToggle(day.date) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = day.displayLabel,
                    style = textStyles.captionBold,
                    color = if (day.selected) Color.White else colors.textSecondary,
                )
            }
        }
    }
}

// =============================================================================
// Step 2 — Measurements
// =============================================================================

@Suppress("LongMethod", "FunctionSignature")
@Composable
private fun MeasurementsStep(
    state: CheckInWizardUiState.Ready,
    onEvent: (CheckInWizardEvent) -> Unit,
) {
    val spacing = LocalStrakkSpacing.current

    SectionLabel(R.string.checkin_wizard_step_measurements)

    MeasurementField(
        label = stringResource(R.string.checkin_wizard_weight),
        value = state.weight,
        delta = state.delta?.weight,
        onChange = { onEvent(CheckInWizardEvent.OnWeightChanged(it)) },
        imeAction = ImeAction.Next,
    )
    MeasurementField(
        label = stringResource(R.string.checkin_wizard_shoulders),
        value = state.shoulders,
        delta = state.delta?.shoulders,
        onChange = { onEvent(CheckInWizardEvent.OnShouldersChanged(it)) },
    )
    MeasurementField(
        label = stringResource(R.string.checkin_wizard_chest),
        value = state.chest,
        delta = state.delta?.chest,
        onChange = { onEvent(CheckInWizardEvent.OnChestChanged(it)) },
    )
    MeasurementField(
        label = stringResource(R.string.checkin_wizard_arm_left),
        value = state.armLeft,
        delta = state.delta?.armLeft,
        onChange = { onEvent(CheckInWizardEvent.OnArmLeftChanged(it)) },
    )
    MeasurementField(
        label = stringResource(R.string.checkin_wizard_arm_right),
        value = state.armRight,
        delta = state.delta?.armRight,
        onChange = { onEvent(CheckInWizardEvent.OnArmRightChanged(it)) },
    )
    MeasurementField(
        label = stringResource(R.string.checkin_wizard_waist),
        value = state.waist,
        delta = state.delta?.waist,
        onChange = { onEvent(CheckInWizardEvent.OnWaistChanged(it)) },
    )
    MeasurementField(
        label = stringResource(R.string.checkin_wizard_hips),
        value = state.hips,
        delta = state.delta?.hips,
        onChange = { onEvent(CheckInWizardEvent.OnHipsChanged(it)) },
    )
    MeasurementField(
        label = stringResource(R.string.checkin_wizard_thigh_left),
        value = state.thighLeft,
        delta = state.delta?.thighLeft,
        onChange = { onEvent(CheckInWizardEvent.OnThighLeftChanged(it)) },
    )
    MeasurementField(
        label = stringResource(R.string.checkin_wizard_thigh_right),
        value = state.thighRight,
        delta = state.delta?.thighRight,
        onChange = { onEvent(CheckInWizardEvent.OnThighRightChanged(it)) },
        imeAction = ImeAction.Done,
    )
}

@Composable
private fun MeasurementField(
    label: String,
    value: String,
    delta: Double?,
    onChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = textStyles.caption,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            if (delta != null && delta != 0.0) {
                val sign = if (delta > 0) "+" else ""
                val deltaColor = if (delta < 0) colors.success else colors.warning
                Text(
                    text = String.format(java.util.Locale.getDefault(), "%s%.1f", sign, delta),
                    style = textStyles.caption,
                    color = deltaColor,
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = imeAction,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accentOrange,
                unfocusedBorderColor = colors.divider,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// =============================================================================
// Step 3 — Feelings
// =============================================================================

@Suppress("LongMethod", "FunctionSignature")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeelingsStep(
    state: CheckInWizardUiState.Ready,
    onEvent: (CheckInWizardEvent) -> Unit,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    SectionLabel(R.string.checkin_wizard_feeling_tags)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        feelingTagOptions.forEach { (slug, labelRes) ->
            val isSelected = slug in state.selectedTags
            @OptIn(ExperimentalMaterial3Api::class)
            FilterChip(
                selected = isSelected,
                onClick = { onEvent(CheckInWizardEvent.OnToggleTag(slug)) },
                label = { Text(stringResource(labelRes), style = textStyles.caption) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.accentOrange,
                    selectedLabelColor = Color.White,
                    containerColor = colors.surface2,
                    labelColor = colors.textSecondary,
                ),
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    SectionLabel(R.string.checkin_wizard_mental_feeling)
    OutlinedTextField(
        value = state.mentalFeeling,
        onValueChange = { onEvent(CheckInWizardEvent.OnMentalFeelingChanged(it)) },
        minLines = 3,
        maxLines = 6,
        placeholder = {
            Text(stringResource(R.string.checkin_wizard_feeling_placeholder), color = colors.textTertiary)
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accentOrange,
            unfocusedBorderColor = colors.divider,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    SectionLabel(R.string.checkin_wizard_physical_feeling)
    OutlinedTextField(
        value = state.physicalFeeling,
        onValueChange = { onEvent(CheckInWizardEvent.OnPhysicalFeelingChanged(it)) },
        minLines = 3,
        maxLines = 6,
        placeholder = {
            Text(stringResource(R.string.checkin_wizard_feeling_placeholder), color = colors.textTertiary)
        },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accentOrange,
            unfocusedBorderColor = colors.divider,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

// =============================================================================
// Step 4 — Photos
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun PhotosStep(
    state: CheckInWizardUiState.Ready,
    onEvent: (CheckInWizardEvent) -> Unit,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val context = LocalContext.current
    val spacing = LocalStrakkSpacing.current

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            // * Read image bytes from the content URI and forward to the ViewModel.
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) onEvent(CheckInWizardEvent.OnAddPhoto(bytes))
        }
    }

    SectionLabel(R.string.checkin_wizard_photos_info)
    Text(
        text = stringResource(R.string.checkin_wizard_photos_count, state.photos.size, MAX_PHOTOS),
        style = textStyles.caption,
        color = colors.textTertiary,
    )

    Spacer(Modifier.height(spacing.xs))

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        state.photos.forEach { photo ->
            PhotoThumbnail(
                photo = photo,
                onRemove = { onEvent(CheckInWizardEvent.OnRemovePhoto(photo.id)) },
                modifier = Modifier.weight(1f),
            )
        }
        if (state.photos.size < MAX_PHOTOS) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(PHOTO_THUMBNAIL_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface2)
                    .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
                    .clickable { photoPicker.launch("image/*") },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = colors.textTertiary)
            }
        }
    }
}

@Suppress("UnusedParameter", "FunctionSignature")
@Composable
private fun PhotoThumbnail(
    photo: WizardPhoto,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Box(
        modifier = modifier
            .height(PHOTO_THUMBNAIL_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface1),
    ) {
        // * Full photo rendering (coil/AsyncImage) is a TODO — shows a placeholder for now.
        // TODO: Render actual photo using Coil AsyncImage once dependency is added.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text("📷", style = MaterialTheme.typography.headlineMedium)
        }

        androidx.compose.material3.IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.common_delete),
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// =============================================================================
// Step 5 — Summary
// =============================================================================

@Suppress("UnusedParameter", "FunctionSignature", "LongMethod")
@Composable
private fun SummaryStep(
    state: CheckInWizardUiState.Ready,
    onEvent: (CheckInWizardEvent) -> Unit,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    SectionLabel(R.string.checkin_wizard_step_summary)

    if (state.nutritionLoading) {
        StrakkLoadingState()
        Text(
            text = stringResource(R.string.checkin_wizard_nutrition_loading),
            style = textStyles.caption,
            color = colors.textTertiary,
        )
    } else {
        state.nutritionSummary?.let { ns ->
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface1)
                    .padding(spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.checkin_wizard_nutrition_over_n_days, ns.nutritionDays).uppercase(),
                    style = textStyles.overline,
                    color = colors.textTertiary,
                )
                NutritionRow(
                    label = stringResource(R.string.checkin_wizard_avg_protein),
                    value = String.format(java.util.Locale.getDefault(), "%.0f g", ns.avgProtein),
                )
                NutritionRow(
                    label = stringResource(R.string.checkin_wizard_avg_calories),
                    value = String.format(java.util.Locale.getDefault(), "%.0f kcal", ns.avgCalories),
                )
                NutritionRow(
                    label = stringResource(R.string.checkin_wizard_avg_fat),
                    value = String.format(java.util.Locale.getDefault(), "%.0f g", ns.avgFat),
                )
                NutritionRow(
                    label = stringResource(R.string.checkin_wizard_avg_carbs),
                    value = String.format(java.util.Locale.getDefault(), "%.0f g", ns.avgCarbs),
                )
                if (ns.avgWater > 0) {
                    NutritionRow(label = stringResource(R.string.checkin_wizard_avg_water), value = "${ns.avgWater} mL")
                }
            }
            ns.aiSummary.takeIf { !it.isNullOrBlank() }?.let { summary ->
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = summary,
                    style = textStyles.body,
                    color = colors.textSecondary,
                )
            }
        } ?: Text(
            text = stringResource(R.string.checkin_wizard_no_nutrition),
            style = textStyles.caption,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun NutritionRow(label: String, value: String) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label, style = textStyles.body, color = colors.textSecondary)
        Text(text = value, style = textStyles.bodyBold, color = colors.textPrimary)
    }
}

// =============================================================================
// Bottom nav bar
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun WizardBottomBar(
    state: CheckInWizardUiState.Ready,
    onEvent: (CheckInWizardEvent) -> Unit,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
    ) {
        StrakkSecondaryButton(
            text = stringResource(R.string.checkin_wizard_back),
            onClick = { onEvent(CheckInWizardEvent.OnBack) },
            modifier = Modifier.weight(1f),
        )
        if (state.currentStep == WizardStep.Summary) {
            StrakkPrimaryButton(
                text = stringResource(R.string.checkin_wizard_save),
                onClick = { onEvent(CheckInWizardEvent.OnSave) },
                enabled = state.canGoNext,
                loading = state.saving,
                modifier = Modifier.weight(2f),
            )
        } else {
            StrakkPrimaryButton(
                text = stringResource(R.string.checkin_wizard_next),
                onClick = { onEvent(CheckInWizardEvent.OnNext) },
                enabled = state.canGoNext,
                modifier = Modifier.weight(2f),
            )
        }
    }
}

// =============================================================================
// Helpers
// =============================================================================

@Composable
private fun SectionLabel(labelRes: Int) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    Text(
        text = stringResource(labelRes).uppercase(),
        style = textStyles.overline,
        color = colors.textTertiary,
    )
}

@Composable
private fun wizardStepLabel(step: WizardStep): String = stringResource(
    when (step) {
        WizardStep.Dates -> R.string.checkin_wizard_step_dates
        WizardStep.Measurements -> R.string.checkin_wizard_step_measurements
        WizardStep.Feelings -> R.string.checkin_wizard_step_feelings
        WizardStep.Photos -> R.string.checkin_wizard_step_photos
        WizardStep.Summary -> R.string.checkin_wizard_step_summary
    },
)

// =============================================================================
// Preview
// =============================================================================

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun CheckInWizardLoadingPreview() {
    StrakkTheme {
        CheckInWizardScreen(
            state = CheckInWizardUiState.Loading,
            onEvent = {},
            snackbar = SnackbarHostState(),
        )
    }
}
