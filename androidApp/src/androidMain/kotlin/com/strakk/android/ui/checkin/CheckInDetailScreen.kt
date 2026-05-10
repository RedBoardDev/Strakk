package com.strakk.android.ui.checkin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.StrakkLoadingState
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.presentation.checkin.CheckInDetailEvent
import com.strakk.shared.presentation.checkin.CheckInDetailUiState

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInDetailScreen(
    state: CheckInDetailUiState,
    onEvent: (CheckInDetailEvent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                showDeleteDialog = false
                onEvent(CheckInDetailEvent.OnConfirmDelete)
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    val title = when (state) {
        CheckInDetailUiState.Loading -> ""
        is CheckInDetailUiState.Ready -> state.checkIn.weekLabel
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) { Snackbar(it) } },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    if (state is CheckInDetailUiState.Ready) {
                        IconButton(onClick = { onEvent(CheckInDetailEvent.OnEdit) }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.checkin_detail_edit),
                                tint = LocalStrakkColors.current.accentOrange,
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.checkin_detail_delete),
                                tint = LocalStrakkColors.current.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        when (state) {
            CheckInDetailUiState.Loading -> StrakkLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is CheckInDetailUiState.Ready -> DetailContent(
                checkIn = state.checkIn,
                delta = state.delta,
                padding = innerPadding,
            )
        }
    }
}

// =============================================================================
// Ready content
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun DetailContent(
    checkIn: CheckIn,
    delta: CheckInDelta?,
    padding: PaddingValues,
) {
    val spacing = LocalStrakkSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        MeasurementsSection(checkIn = checkIn, delta = delta)
        val hasFeelingsData = checkIn.feelingTags.isNotEmpty() ||
            !checkIn.mentalFeeling.isNullOrBlank() ||
            !checkIn.physicalFeeling.isNullOrBlank()
        if (hasFeelingsData) {
            FeelingsSection(checkIn = checkIn)
        }
        if (checkIn.nutritionSummary != null) {
            NutritionSummarySection(checkIn = checkIn)
        }
        Spacer(Modifier.height(spacing.lg))
    }
}

// =============================================================================
// Measurements section
// =============================================================================

@Suppress("LongMethod", "FunctionSignature")
@Composable
private fun MeasurementsSection(checkIn: CheckIn, delta: CheckInDelta?) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.checkin_detail_measurements).uppercase(),
            style = textStyles.overline,
            color = colors.textTertiary,
        )

        checkIn.weight?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_weight),
                value = it,
                delta = delta?.weight,
                unit = "kg",
            )
        }
        checkIn.shoulders?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_shoulders),
                value = it,
                delta = delta?.shoulders,
                unit = "cm",
            )
        }
        checkIn.chest?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_chest),
                value = it,
                delta = delta?.chest,
                unit = "cm",
            )
        }
        checkIn.armLeft?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_arm_left),
                value = it,
                delta = delta?.armLeft,
                unit = "cm",
            )
        }
        checkIn.armRight?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_arm_right),
                value = it,
                delta = delta?.armRight,
                unit = "cm",
            )
        }
        checkIn.waist?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_waist),
                value = it,
                delta = delta?.waist,
                unit = "cm",
            )
        }
        checkIn.hips?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_hips),
                value = it,
                delta = delta?.hips,
                unit = "cm",
            )
        }
        checkIn.thighLeft?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_thigh_left),
                value = it,
                delta = delta?.thighLeft,
                unit = "cm",
            )
        }
        checkIn.thighRight?.let {
            MeasurementRow(
                label = stringResource(R.string.checkin_wizard_thigh_right),
                value = it,
                delta = delta?.thighRight,
                unit = "cm",
            )
        }

        if (listOf(checkIn.weight, checkIn.shoulders, checkIn.chest, checkIn.waist).all { it == null }) {
            Text(
                text = stringResource(R.string.checkin_detail_no_measurements),
                style = textStyles.caption,
                color = colors.textTertiary,
            )
        }
    }
}

@Suppress("FunctionSignature")
@Composable
private fun MeasurementRow(
    label: String,
    value: Double,
    delta: Double?,
    unit: String,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val locale = java.util.Locale.getDefault()

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label, style = textStyles.body, color = colors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = String.format(locale, "%.1f %s", value, unit),
                style = textStyles.bodyBold,
                color = colors.textPrimary,
            )
            if (delta != null && delta != 0.0) {
                val sign = if (delta > 0) "+" else ""
                val deltaColor = if (delta < 0) colors.success else colors.warning
                Text(
                    text = String.format(locale, "%s%.1f", sign, delta),
                    style = textStyles.caption,
                    color = deltaColor,
                )
            }
        }
    }
}

// =============================================================================
// Feelings section
// =============================================================================

@Suppress("LongMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeelingsSection(checkIn: CheckIn) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.checkin_detail_feelings).uppercase(),
            style = textStyles.overline,
            color = colors.textTertiary,
        )

        if (checkIn.feelingTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                checkIn.feelingTags.forEach { slug ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.accentOrangeFaint)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = slug,
                            style = textStyles.caption,
                            color = colors.accentOrange,
                        )
                    }
                }
            }
        }

        checkIn.mentalFeeling.takeIf { !it.isNullOrBlank() }?.let { feeling ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.checkin_wizard_mental_feeling),
                    style = textStyles.captionBold,
                    color = colors.textSecondary,
                )
                Text(text = feeling, style = textStyles.body, color = colors.textPrimary)
            }
        }

        checkIn.physicalFeeling.takeIf { !it.isNullOrBlank() }?.let { feeling ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.checkin_wizard_physical_feeling),
                    style = textStyles.captionBold,
                    color = colors.textSecondary,
                )
                Text(text = feeling, style = textStyles.body, color = colors.textPrimary)
            }
        }
    }
}

// =============================================================================
// Nutrition summary section
// =============================================================================

@Composable
private fun NutritionSummarySection(checkIn: CheckIn) {
    val ns = checkIn.nutritionSummary ?: return
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface1)
            .padding(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.checkin_detail_nutrition).uppercase(),
            style = textStyles.overline,
            color = colors.textTertiary,
        )
        val locale = java.util.Locale.getDefault()
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.checkin_wizard_avg_protein),
                style = textStyles.body,
                color = colors.textSecondary,
            )
            Text(
                text = String.format(locale, "%.0f g", ns.avgProtein),
                style = textStyles.bodyBold,
                color = colors.textPrimary,
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.checkin_wizard_avg_calories),
                style = textStyles.body,
                color = colors.textSecondary,
            )
            Text(
                text = String.format(locale, "%.0f kcal", ns.avgCalories),
                style = textStyles.bodyBold,
                color = colors.textPrimary,
            )
        }
        ns.aiSummary.takeIf { !it.isNullOrBlank() }?.let { summary ->
            Spacer(Modifier.height(4.dp))
            Text(text = summary, style = textStyles.body, color = colors.textSecondary)
        }
    }
}

// =============================================================================
// Delete confirmation dialog
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.checkin_detail_delete_confirm_title)) },
        text = { Text(stringResource(R.string.checkin_detail_delete_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.checkin_detail_delete_confirm),
                    color = LocalStrakkColors.current.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        containerColor = LocalStrakkColors.current.surface1,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        textContentColor = LocalStrakkColors.current.textSecondary,
    )
}

// =============================================================================
// Preview
// =============================================================================

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun CheckInDetailLoadingPreview() {
    StrakkTheme {
        CheckInDetailScreen(
            state = CheckInDetailUiState.Loading,
            onEvent = {},
            onNavigateBack = {},
            snackbar = SnackbarHostState(),
        )
    }
}
