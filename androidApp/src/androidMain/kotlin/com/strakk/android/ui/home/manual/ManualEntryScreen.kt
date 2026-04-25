package com.strakk.android.ui.home.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.presentation.meal.ManualEntryEffect
import com.strakk.shared.presentation.meal.ManualEntryEvent
import com.strakk.shared.presentation.meal.ManualEntryUiState
import com.strakk.shared.presentation.meal.ManualEntryViewModel
import org.koin.compose.viewmodel.koinViewModel

// =============================================================================
// Route
// =============================================================================

@Composable
fun ManualEntryRoute(
    onDismiss: () -> Unit,
    onAdded: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManualEntryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ManualEntryEffect.Submitted -> onAdded(effect.entry)
                is ManualEntryEffect.Cancelled -> onDismiss()
                is ManualEntryEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ManualEntryScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

// =============================================================================
// Screen
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntryScreen(
    uiState: ManualEntryUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ManualEntryEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajout manuel",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Fermer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = { onEvent(ManualEntryEvent.Submit) },
                    enabled = uiState.isSubmittable && !uiState.isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = LocalStrakkColors.current.surface2,
                        disabledContentColor = LocalStrakkColors.current.textTertiary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Ajouter")
                    }
                }
            }
        },
    ) { innerPadding ->
        ManualEntryForm(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        )
    }
}

// =============================================================================
// Form
// =============================================================================

@Composable
private fun ManualEntryForm(
    uiState: ManualEntryUiState,
    onEvent: (ManualEntryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    // Track which fields have lost focus (for inline validation)
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    var proteinTouched by rememberSaveable { mutableStateOf(false) }
    var caloriesTouched by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        // Name (required)
        FormField(
            label = "Nom *",
            value = uiState.name,
            onValueChange = { onEvent(ManualEntryEvent.NameChanged(it)) },
            isError = nameTouched && uiState.name.isBlank(),
            errorMessage = "Le nom est requis",
            onFocusLost = { nameTouched = true },
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )

        // Protein (required)
        FormField(
            label = "Protéines (g) *",
            value = uiState.protein,
            onValueChange = { onEvent(ManualEntryEvent.ProteinChanged(it)) },
            isError = proteinTouched && uiState.protein.toDoubleOrNull() == null,
            errorMessage = "Valeur numérique requise",
            onFocusLost = { proteinTouched = true },
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )

        // Calories (required)
        FormField(
            label = "Calories (kcal) *",
            value = uiState.calories,
            onValueChange = { onEvent(ManualEntryEvent.CaloriesChanged(it)) },
            isError = caloriesTouched && uiState.calories.toDoubleOrNull() == null,
            errorMessage = "Valeur numérique requise",
            onFocusLost = { caloriesTouched = true },
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )

        // Fat (optional)
        FormField(
            label = "Lipides (g)",
            value = uiState.fat,
            onValueChange = { onEvent(ManualEntryEvent.FatChanged(it)) },
            isError = false,
            errorMessage = null,
            onFocusLost = {},
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )

        // Carbs (optional)
        FormField(
            label = "Glucides (g)",
            value = uiState.carbs,
            onValueChange = { onEvent(ManualEntryEvent.CarbsChanged(it)) },
            isError = false,
            errorMessage = null,
            onFocusLost = {},
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )

        // Quantity (optional, text)
        FormField(
            label = "Quantité",
            value = uiState.quantity,
            onValueChange = { onEvent(ManualEntryEvent.QuantityChanged(it)) },
            isError = false,
            errorMessage = null,
            onFocusLost = {},
            placeholder = "ex : 150g ou 1 bol",
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
        )

        // Global error message
        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = LocalStrakkColors.current.error,
            )
        }
    }
}

// =============================================================================
// Generic form field
// =============================================================================

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) LocalStrakkColors.current.error else LocalStrakkColors.current.textSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            isError = isError,
            singleLine = true,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(text = placeholder, color = LocalStrakkColors.current.textTertiary) }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() },
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = LocalStrakkColors.current.divider,
                errorBorderColor = LocalStrakkColors.current.error,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) onFocusLost()
                },
        )
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = LocalStrakkColors.current.error,
            )
        }
    }
}


// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun ManualEntryScreenPreview() {
    StrakkTheme {
        ManualEntryScreen(
            uiState = ManualEntryUiState(
                name = "Poulet grillé",
                protein = "30",
                calories = "165",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun ManualEntryScreenEmptyPreview() {
    StrakkTheme {
        ManualEntryScreen(
            uiState = ManualEntryUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onDismiss = {},
        )
    }
}
