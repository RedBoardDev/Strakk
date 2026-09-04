package com.strakk.android.ui.home.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.R
import com.strakk.android.ui.components.StrakkEmptyState
import com.strakk.android.ui.components.StrakkLoadingState
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.presentation.meal.SearchFoodEffect
import com.strakk.shared.presentation.meal.SearchFoodEvent
import com.strakk.shared.presentation.meal.SearchFoodUiState
import com.strakk.shared.presentation.meal.SearchFoodViewModel
import com.strakk.shared.presentation.meal.SearchTab
import org.koin.compose.viewmodel.koinViewModel

// =============================================================================
// Route
// =============================================================================

@Composable
fun SearchFoodRoute(
    onNavigateBack: () -> Unit,
    onConfirm: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchFoodViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SearchFoodEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                else -> { /* not surfaced here */ }
            }
        }
    }

    SearchFoodScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onConfirm = onConfirm,
        modifier = modifier,
    )
}

// =============================================================================
// Screen
// =============================================================================

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFoodScreen(
    uiState: SearchFoodUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (SearchFoodEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onConfirm: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { SearchTopField(uiState, onEvent, focusRequester) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.search_food_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is SearchFoodUiState.Loading ->
                    StrakkLoadingState(modifier = Modifier.fillMaxSize())
                is SearchFoodUiState.Error ->
                    StrakkEmptyState(
                        title = stringResource(R.string.search_food_error_title),
                        description = state.message,
                        actionLabel = stringResource(R.string.search_food_retry),
                        onAction = { onEvent(SearchFoodEvent.Retry) },
                        modifier = Modifier.fillMaxSize(),
                    )
                is SearchFoodUiState.Ready ->
                    SearchFoodContent(
                        state = state,
                        onEvent = onEvent,
                        onConfirm = onConfirm,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopField(
    uiState: SearchFoodUiState,
    onEvent: (SearchFoodEvent) -> Unit,
    focusRequester: FocusRequester,
) {
    val query = (uiState as? SearchFoodUiState.Ready)?.query ?: ""
    OutlinedTextField(
        value = query,
        onValueChange = { onEvent(SearchFoodEvent.QueryChanged(it)) },
        placeholder = {
            Text(
                text = stringResource(R.string.search_food_placeholder),
                color = LocalStrakkColors.current.textTertiary,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { /* search already live */ }),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onEvent(SearchFoodEvent.QueryChanged("")) }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.search_food_clear),
                        tint = LocalStrakkColors.current.textTertiary,
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = LocalStrakkColors.current.textTertiary,
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = LocalStrakkColors.current.divider,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

// =============================================================================
// Content
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFoodContent(
    state: SearchFoodUiState.Ready,
    onEvent: (SearchFoodEvent) -> Unit,
    onConfirm: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedUserItemName by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedCatalogItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedTabIndex = if (state.selectedTab == SearchTab.Catalog) 1 else 0

    LazyColumn(modifier = modifier) {
        item {
            SecondaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { onEvent(SearchFoodEvent.SwitchTab(SearchTab.MyFoods)) },
                    text = { Text(stringResource(R.string.search_food_tab_my_foods)) },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { onEvent(SearchFoodEvent.SwitchTab(SearchTab.Catalog)) },
                    text = { Text(stringResource(R.string.search_food_tab_catalog)) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.selectedTab == SearchTab.Catalog) {
            catalogSection(
                state = state,
                expandedCatalogItemId = expandedCatalogItemId,
                onExpand = { id -> expandedCatalogItemId = if (expandedCatalogItemId == id) null else id },
                onConfirm = { entry ->
                    onConfirm(entry)
                    expandedCatalogItemId = null
                },
            )
        } else {
            myFoodsSection(
                state = state,
                expandedUserItemName = expandedUserItemName,
                onExpand = { name -> expandedUserItemName = if (expandedUserItemName == name) null else name },
                onConfirm = { entry ->
                    onConfirm(entry)
                    expandedUserItemName = null
                },
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.catalogSection(
    state: SearchFoodUiState.Ready,
    expandedCatalogItemId: Long?,
    onExpand: (Long) -> Unit,
    onConfirm: (MealEntry) -> Unit,
) {
    if (state.query.isEmpty()) {
        item {
            StrakkEmptyState(
                title = "Search the catalog",
                description = "Type a food name to query CIQUAL / OFF.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
            )
        }
        return
    }

    if (state.isSearching && state.catalog.items.isEmpty()) {
        item { StrakkLoadingState(modifier = Modifier.padding(vertical = 32.dp)) }
        return
    }

    if (state.catalog.items.isEmpty()) {
        item {
            StrakkEmptyState(
                title = "No results for \"${state.query}\"",
                description = "Try a different word, or add it manually.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
            )
        }
        return
    }

    item {
        SectionOverline(text = "CATALOG", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
    }
    items(items = state.catalog.items, key = { "catalog-${it.id}" }) { item ->
        CatalogItemRow(
            item = item,
            isExpanded = expandedCatalogItemId == item.id,
            onTap = { onExpand(item.id) },
            onConfirm = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.myFoodsSection(
    state: SearchFoodUiState.Ready,
    expandedUserItemName: String?,
    onExpand: (String) -> Unit,
    onConfirm: (MealEntry) -> Unit,
) {
    val recent = state.myFoods.recentFoods
    if (recent.isEmpty() && state.myFoods.favoriteFoods.isEmpty()) {
        item {
            StrakkEmptyState(
                title = if (state.query.isEmpty()) "Nothing saved yet" else "No results for \"${state.query}\"",
                description = "Foods you log will show up here.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
            )
        }
        return
    }

    if (state.myFoods.favoriteFoods.isNotEmpty()) {
        item {
            SectionOverline(text = "FAVORITES", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
    }
    if (recent.isNotEmpty()) {
        item {
            SectionOverline(
                text = "RECENT FOODS",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        items(items = recent, key = { "freq-${it.normalizedName}" }) { item ->
            FrequentItemRow(
                item = item,
                isExpanded = expandedUserItemName == item.normalizedName,
                onTap = { onExpand(item.normalizedName) },
                onConfirm = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
            )
        }
    }
}
