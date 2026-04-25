package com.strakk.android.ui.home.draft

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.home.add.AddPickerSheet
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.MealEntry
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

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MealDraftEffect.NavigateToReview -> onNavigateToReview()
                is MealDraftEffect.Committed -> {
                    snackbarHostState.showSnackbar("Repas enregistré")
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
                    TextButton(
                        onClick = { showRenameDialog = true },
                    ) {
                        Text(
                            text = editingState?.draft?.name ?: "Nouveau repas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Renommer") },
                            onClick = {
                                showMenu = false
                                showRenameDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Annuler le repas", color = LocalStrakkColors.current.error) },
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
                            text = "Aucun repas en cours",
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
                            Text("Démarrer un repas")
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

    // Add picker bottom sheet
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

    // Rename dialog
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

    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Annuler le repas ?") },
            text = { Text("Toutes les entrées seront perdues.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onEvent(MealDraftEvent.Discard)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalStrakkColors.current.error,
                    ),
                ) {
                    Text("Annuler le repas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Garder")
                }
            },
        )
    }
}

// =============================================================================
// Items list
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftItemsList(
    state: MealDraftUiState.Editing,
    onRemoveItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
    ) {
        // Totals card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            TotalsCard(state = state)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ITEMS",
                style = MaterialTheme.typography.labelSmall,
                color = LocalStrakkColors.current.textTertiary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.draft.items.isEmpty()) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                ) {
                    Text(
                        text = "Aucun item — tape + Ajouter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalStrakkColors.current.textSecondary,
                    )
                }
            }
        } else {
            items(
                items = state.draft.items,
                key = { it.id },
            ) { item ->
                val dismissState = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        onRemoveItem(item.id)
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Surface(
                            color = LocalStrakkColors.current.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 16.dp),
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Supprimer",
                                    tint = LocalStrakkColors.current.error,
                                )
                            }
                        }
                    },
                    enableDismissFromStartToEnd = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                ) {
                    DraftItemRow(
                        item = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun TotalsCard(
    state: MealDraftUiState.Editing,
    modifier: Modifier = Modifier,
) {
    val totals = state.totals
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            MacroCell(
                label = "Protéines",
                value = "${totals.protein.toInt()}g",
                color = MaterialTheme.colorScheme.primary,
            )
            MacroCell(
                label = "Calories",
                value = "${totals.calories.toInt()} kcal",
                color = LocalStrakkColors.current.calories,
            )
            MacroCell(
                label = "Lipides",
                value = "${totals.fat.toInt()}g",
                color = LocalStrakkColors.current.textSecondary,
            )
            MacroCell(
                label = "Glucides",
                value = "${totals.carbs.toInt()}g",
                color = LocalStrakkColors.current.textSecondary,
            )
        }
    }
}

@Composable
private fun MacroCell(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalStrakkColors.current.textTertiary,
        )
    }
}

@Composable
private fun DraftItemRow(
    item: DraftItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        when (item) {
            is DraftItem.Resolved -> ResolvedItemRow(item = item)
            is DraftItem.PendingPhoto -> PendingPhotoRow(item = item)
            is DraftItem.PendingText -> PendingTextRow(item = item)
        }
    }
}

@Composable
private fun ResolvedItemRow(
    item: DraftItem.Resolved,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.entry.name ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.entry.quantity?.let { qty ->
                Text(
                    text = qty,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalStrakkColors.current.textSecondary,
                )
            }
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("${item.entry.protein.toInt()}g")
                }
                withStyle(SpanStyle(color = LocalStrakkColors.current.textSecondary)) {
                    append(" · ${item.entry.calories.toInt()} kcal")
                }
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PendingPhotoRow(
    item: DraftItem.PendingPhoto,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // Thumbnail
        val bitmap = remember(item.imageBase64) {
            try {
                val bytes = Base64.decode(item.imageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.size(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.hint ?: "Photo sans indice",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PendingBadge(label = "📷 En attente d'analyse")
        }
    }
}

@Composable
private fun PendingTextRow(
    item: DraftItem.PendingText,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            PendingBadge(label = "📝 En attente d'analyse")
        }
    }
}

@Composable
private fun PendingBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LocalStrakkColors.current.warning.copy(alpha = 0.15f),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalStrakkColors.current.warning,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// =============================================================================
// Bottom bar
// =============================================================================

@Composable
private fun DraftBottomBar(
    editingState: MealDraftUiState.Editing,
    onAdd: () -> Unit,
    onProcess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalStrakkColors.current.surface2,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text("+ Ajouter")
                }
                Button(
                    onClick = onProcess,
                    enabled = editingState.resolvedCount > 0 || editingState.pendingCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                ) {
                    if (editingState.isProcessing) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text("Terminer le repas")
                    }
                }
            }
        }
    }
}

// =============================================================================
// Rename dialog
// =============================================================================

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renommer le repas") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Nom du repas") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim().ifBlank { currentName }) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Renommer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        modifier = modifier,
    )
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
