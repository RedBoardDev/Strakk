package com.strakk.android.ui.home.draft

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.R
import com.strakk.android.ui.home.add.AddPickerSheet
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.meal.MealDraftEffect
import com.strakk.shared.presentation.meal.MealDraftEvent
import com.strakk.shared.presentation.meal.MealDraftUiState
import com.strakk.shared.presentation.meal.MealDraftViewModel
import org.koin.compose.viewmodel.koinViewModel

// =============================================================================
// Route
// =============================================================================

@Composable
fun MealDraftRoute(
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToSearch: (inDraft: Boolean) -> Unit,
    onNavigateToManual: (inDraft: Boolean) -> Unit,
    onNavigateToPhoto: (inDraft: Boolean) -> Unit,
    onNavigateToText: (inDraft: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealDraftViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MealDraftEffect.Started -> Unit
                is MealDraftEffect.NavigateToReview -> onNavigateToReview()
                is MealDraftEffect.Committed -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.meal_draft_meal_saved))
                    onNavigateBack()
                }
                is MealDraftEffect.Discarded -> onNavigateBack()
                is MealDraftEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    MealDraftScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToSearch = { onNavigateToSearch(true) },
        onNavigateToManual = { onNavigateToManual(true) },
        onNavigateToPhoto = { onNavigateToPhoto(true) },
        onNavigateToText = { onNavigateToText(true) },
        modifier = modifier,
    )
}

// =============================================================================
// Screen
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealDraftScreen(
    uiState: MealDraftUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (MealDraftEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToPhoto: () -> Unit,
    onNavigateToText: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val editingState = uiState as? MealDraftUiState.Editing

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    TextButton(onClick = { showRenameDialog = true }) {
                        Text(
                            text = editingState?.draft?.name ?: stringResource(R.string.meal_draft_new_meal),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.meal_draft_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.meal_draft_menu))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.meal_draft_rename)) },
                            onClick = {
                                showMenu = false
                                showRenameDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.meal_draft_cancel_meal), color = LocalStrakkColors.current.error) },
                            onClick = {
                                showMenu = false
                                showDiscardDialog = true
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            when (val state = uiState) {
                is MealDraftUiState.Editing -> {
                    DraftBottomBar(
                        editingState = state,
                        onAdd = { showAddPicker = true },
                        onProcess = { onEvent(MealDraftEvent.Process) },
                    )
                }
                else -> {}
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is MealDraftUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is MealDraftUiState.Empty -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = stringResource(R.string.meal_draft_no_meal),
                            style = MaterialTheme.typography.bodyLarge,
                            color = LocalStrakkColors.current.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onEvent(MealDraftEvent.StartDraft()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(stringResource(R.string.meal_draft_start_meal))
                        }
                    }
                }
                is MealDraftUiState.Editing -> {
                    DraftItemsList(
                        state = state,
                        onRemoveItem = { onEvent(MealDraftEvent.RemoveItem(it)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    if (showAddPicker) {
        AddPickerSheet(
            draftName = editingState?.draft?.name,
            onSearch = {
                showAddPicker = false
                onNavigateToSearch()
            },
            onManual = {
                showAddPicker = false
                onNavigateToManual()
            },
            onText = {
                showAddPicker = false
                onNavigateToText()
            },
            onPhoto = {
                showAddPicker = false
                onNavigateToPhoto()
            },
            onDismiss = { showAddPicker = false },
        )
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = editingState?.draft?.name ?: "",
            onConfirm = { name ->
                onEvent(MealDraftEvent.Rename(name))
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    if (showDiscardDialog) {
        DiscardDialog(
            onConfirm = {
                showDiscardDialog = false
                onEvent(MealDraftEvent.Discard)
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun MealDraftScreenEmptyPreview() {
    StrakkTheme {
        MealDraftScreen(
            uiState = MealDraftUiState.Empty,
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
            onNavigateToSearch = {},
            onNavigateToManual = {},
            onNavigateToPhoto = {},
            onNavigateToText = {},
        )
    }
}
