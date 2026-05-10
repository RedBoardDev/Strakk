package com.strakk.android.ui.home.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.presentation.meal.MealDraftEffect
import com.strakk.shared.presentation.meal.MealDraftEvent
import com.strakk.shared.presentation.meal.MealDraftUiState
import com.strakk.shared.presentation.meal.MealDraftViewModel
import org.koin.compose.viewmodel.koinViewModel

// =============================================================================
// Helpers
// =============================================================================

private const val HIDDEN_ITEM_ALPHA = 0.5f

private fun DraftItem.Resolved.isFromScan(): Boolean =
    entry.source == EntrySource.PhotoAi || entry.source == EntrySource.TextAi

// =============================================================================
// Route
// =============================================================================

@Suppress("LongParameterList")
@Composable
fun MealReviewRoute(
    onNavigateBack: () -> Unit,
    onCommitted: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToManual: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealDraftViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MealDraftEffect.Committed -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.review_committed_snackbar))
                    onCommitted()
                }
                is MealDraftEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                else -> {}
            }
        }
    }

    MealReviewScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToManual = onNavigateToManual,
        modifier = modifier,
    )
}

// =============================================================================
// Screen
// =============================================================================

@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealReviewScreen(
    uiState: MealDraftUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEvent: (MealDraftEvent) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToManual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editingState = uiState as? MealDraftUiState.Editing

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.review_back_cd),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (editingState != null) {
                ReviewBottomBar(
                    state = editingState,
                    onCommit = { onEvent(MealDraftEvent.Commit) },
                )
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
                    Text(
                        text = stringResource(R.string.review_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalStrakkColors.current.textSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                is MealDraftUiState.Editing -> {
                    ReviewContent(
                        state = state,
                        onEvent = onEvent,
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToManual = onNavigateToManual,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

// =============================================================================
// Bottom bar
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun ReviewBottomBar(
    state: MealDraftUiState.Editing,
    onCommit: () -> Unit,
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
            AnimatedVisibility(
                visible = state.hiddenCount > 0,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = stringResource(R.string.review_hidden_warning, state.hiddenCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalStrakkColors.current.warning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = onCommit,
                enabled = state.hasVisibleItems,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = LocalStrakkColors.current.surface2,
                    disabledContentColor = LocalStrakkColors.current.textDisabled,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = if (state.hasVisibleItems) {
                        stringResource(R.string.review_confirm_button)
                    } else {
                        stringResource(R.string.review_all_hidden_disabled)
                    },
                )
            }
        }
    }
}

// =============================================================================
// Content
// =============================================================================

@Suppress("LongMethod", "FunctionSignature")
@Composable
private fun ReviewContent(
    state: MealDraftUiState.Editing,
    onEvent: (MealDraftEvent) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToManual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddPicker by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<DraftItem.Resolved?>(null) }

    val resolvedItems = remember(state.draft.items) {
        state.draft.items.filterIsInstance<DraftItem.Resolved>()
    }

    LazyColumn(modifier = modifier) {
        item(key = "totals") {
            Spacer(modifier = Modifier.height(12.dp))
            ReviewTotalsCard(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "section_header") {
            ItemsSectionHeader(
                onAddClick = { showAddPicker = true },
            )
        }

        items(
            items = resolvedItems,
            key = { it.id },
        ) { item ->
            val isHidden = item.id in state.hiddenItemIds
            ReviewItemCard(
                item = item,
                isHidden = isHidden,
                onHide = { onEvent(MealDraftEvent.HideItem(item.id)) },
                onRestore = { onEvent(MealDraftEvent.RestoreItem(item.id)) },
                onDelete = { onEvent(MealDraftEvent.RemoveItem(item.id)) },
                onEdit = { itemToEdit = item },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .animateItem(),
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showAddPicker) {
        ReviewAddPickerSheet(
            onSearch = {
                showAddPicker = false
                onNavigateToSearch()
            },
            onManual = {
                showAddPicker = false
                onNavigateToManual()
            },
            onDismiss = { showAddPicker = false },
        )
    }

    itemToEdit?.let { item ->
        EditReviewItemSheet(
            item = item,
            onSave = { event ->
                onEvent(event)
                itemToEdit = null
            },
            onDismiss = { itemToEdit = null },
        )
    }
}

// =============================================================================
// TotalsCard
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun ReviewTotalsCard(
    state: MealDraftUiState.Editing,
    modifier: Modifier = Modifier,
) {
    val totals = state.totals
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MacroCell(
                    label = stringResource(R.string.meal_draft_macro_protein),
                    value = "${totals.protein.toInt()}g",
                    color = MaterialTheme.colorScheme.primary,
                )
                MacroCell(
                    label = stringResource(R.string.meal_draft_macro_calories),
                    value = "${totals.calories.toInt()}",
                    color = LocalStrakkColors.current.calories,
                )
                MacroCell(
                    label = stringResource(R.string.meal_draft_macro_fat),
                    value = "${totals.fat.toInt()}g",
                    color = LocalStrakkColors.current.accentYellow,
                )
                MacroCell(
                    label = stringResource(R.string.meal_draft_macro_carbs),
                    value = "${totals.carbs.toInt()}g",
                    color = LocalStrakkColors.current.accentIndigo,
                )
            }

            if (state.hiddenCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.review_totals_hidden_caption, state.hiddenCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalStrakkColors.current.warning,
                )
            }
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

// =============================================================================
// Section header
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun ItemsSectionHeader(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.review_items_section),
            style = MaterialTheme.typography.labelSmall,
            color = LocalStrakkColors.current.textTertiary,
        )
        IconButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.review_add_item_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .background(LocalStrakkColors.current.surface2, CircleShape)
                    .padding(4.dp),
            )
        }
    }
}

// =============================================================================
// ReviewItemCard
// =============================================================================

@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewItemCard(
    item: DraftItem.Resolved,
    isHidden: Boolean,
    onHide: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isHidden) {
        HiddenItemCard(
            item = item,
            onRestore = onRestore,
            modifier = modifier,
        )
        return
    }

    var showMenu by remember { mutableStateOf(false) }
    val isScan = item.isFromScan()

    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                if (isScan) onHide() else onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = swipeState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (isScan) {
                LocalStrakkColors.current.accentYellowFaint
            } else {
                LocalStrakkColors.current.error.copy(alpha = 0.15f)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = if (isScan) Icons.Outlined.VisibilityOff else Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = if (isScan) LocalStrakkColors.current.accentYellow else LocalStrakkColors.current.error,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.entry.name ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        Text(
                            text = "${item.entry.protein.toInt()}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = " · ${item.entry.calories.toInt()} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalStrakkColors.current.textSecondary,
                        )
                        item.entry.quantity?.let { qty ->
                            Text(
                                text = " · $qty",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalStrakkColors.current.textTertiary,
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null,
                            tint = LocalStrakkColors.current.textTertiary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.review_edit_item)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                        )
                        if (isScan) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.review_hide_item)) },
                                onClick = {
                                    showMenu = false
                                    onHide()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.review_delete_item),
                                        color = LocalStrakkColors.current.error,
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// HiddenItemCard
// =============================================================================

@Suppress("FunctionSignature")
@Composable
private fun HiddenItemCard(
    item: DraftItem.Resolved,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.alpha(HIDDEN_ITEM_ALPHA),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.VisibilityOff,
                contentDescription = null,
                tint = LocalStrakkColors.current.textTertiary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.entry.name ?: "—",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = TextDecoration.LineThrough,
                ),
                color = LocalStrakkColors.current.textSecondary,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRestore,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = LocalStrakkColors.current.warning,
                ),
            ) {
                Text(
                    text = stringResource(R.string.review_restore_item),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

// =============================================================================
// Add picker sheet (Search + Manual only)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewAddPickerSheet(
    onSearch: () -> Unit,
    onManual: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalStrakkColors.current.surface2,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Surface(
                onClick = onSearch,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.review_add_search),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                onClick = onManual,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.review_add_manual),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// =============================================================================
// EditReviewItemSheet
// =============================================================================

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditReviewItemSheet(
    item: DraftItem.Resolved,
    onSave: (MealDraftEvent.UpdateResolvedItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(item.entry.name.orEmpty()) }
    var protein by remember { mutableStateOf(item.entry.protein.toInt().toString()) }
    var calories by remember { mutableStateOf(item.entry.calories.toInt().toString()) }
    var fat by remember { mutableStateOf(item.entry.fat?.toInt()?.toString().orEmpty()) }
    var carbs by remember { mutableStateOf(item.entry.carbs?.toInt()?.toString().orEmpty()) }
    var quantity by remember { mutableStateOf(item.entry.quantity.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalStrakkColors.current.surface2,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.review_edit_item),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.manual_entry_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text(stringResource(R.string.manual_entry_field_protein)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text(stringResource(R.string.manual_entry_field_calories)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text(stringResource(R.string.manual_entry_field_fat)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text(stringResource(R.string.manual_entry_field_carbs)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text(stringResource(R.string.manual_entry_field_quantity)) },
                placeholder = { Text(stringResource(R.string.manual_entry_quantity_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val proteinVal = protein.toDoubleOrNull() ?: item.entry.protein
                    val caloriesVal = calories.toDoubleOrNull() ?: item.entry.calories
                    val fatVal = fat.toDoubleOrNull() ?: item.entry.fat
                    val carbsVal = carbs.toDoubleOrNull() ?: item.entry.carbs
                    onSave(
                        MealDraftEvent.UpdateResolvedItem(
                            itemId = item.id,
                            name = name.ifBlank { item.entry.name.orEmpty() },
                            protein = proteinVal,
                            calories = caloriesVal,
                            fat = fatVal,
                            carbs = carbsVal,
                            quantity = quantity.ifBlank { null },
                            source = item.entry.source,
                            createdAt = item.entry.createdAt,
                        ),
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.common_save))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
internal fun MealReviewScreenPreview() {
    StrakkTheme {
        MealReviewScreen(
            uiState = MealDraftUiState.Empty,
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onEvent = {},
            onNavigateToSearch = {},
            onNavigateToManual = {},
        )
    }
}
