package com.strakk.android.ui.home.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.usecase.SearchFoodUseCase.SearchResults
import com.strakk.shared.presentation.meal.SearchFoodEffect
import com.strakk.shared.presentation.meal.SearchFoodEvent
import com.strakk.shared.presentation.meal.SearchFoodUiState
import com.strakk.shared.presentation.meal.SearchFoodViewModel
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
                is SearchFoodEffect.ItemSelected -> { /* handled in composable */ }
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val query = (uiState as? SearchFoodUiState.Ready)?.query ?: ""
                    OutlinedTextField(
                        value = query,
                        onValueChange = { onEvent(SearchFoodEvent.QueryChanged(it)) },
                        placeholder = {
                            Text(
                                text = "Rechercher un aliment…",
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
                                        contentDescription = "Effacer",
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
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Retour")
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
                is SearchFoodUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is SearchFoodUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalStrakkColors.current.error,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onEvent(SearchFoodEvent.Retry) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("Réessayer")
                        }
                    }
                }
                is SearchFoodUiState.Ready -> {
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
}

// =============================================================================
// Content
// =============================================================================

@Composable
private fun SearchFoodContent(
    state: SearchFoodUiState.Ready,
    onEvent: (SearchFoodEvent) -> Unit,
    onConfirm: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track which item is expanded inline (null = none)
    var expandedUserItemName by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedCatalogItemId by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
    ) {
        if (state.query.isEmpty()) {
            // Section FRÉQUENTS
            if (state.results.userItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionOverline(text = "FRÉQUENTS")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(
                    items = state.results.userItems,
                    key = { it.normalizedName },
                ) { item ->
                    FrequentItemRow(
                        item = item,
                        isExpanded = expandedUserItemName == item.normalizedName,
                        onTap = {
                            expandedUserItemName = if (expandedUserItemName == item.normalizedName) null else item.normalizedName
                            expandedCatalogItemId = null
                        },
                        onConfirm = { entry ->
                            onConfirm(entry)
                            expandedUserItemName = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .animateItem(),
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Commencez à taper pour rechercher",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalStrakkColors.current.textSecondary,
                        )
                    }
                }
            }
        } else {
            // Section MES ALIMENTS
            if (state.results.userItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionOverline(text = "MES ALIMENTS")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(
                    items = state.results.userItems,
                    key = { "user-${it.normalizedName}" },
                ) { item ->
                    FrequentItemRow(
                        item = item,
                        isExpanded = expandedUserItemName == item.normalizedName,
                        onTap = {
                            expandedUserItemName = if (expandedUserItemName == item.normalizedName) null else item.normalizedName
                            expandedCatalogItemId = null
                        },
                        onConfirm = { entry ->
                            onConfirm(entry)
                            expandedUserItemName = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .animateItem(),
                    )
                }
            }

            // Section CATALOGUE
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionOverline(text = "CATALOGUE")
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (state.isSearching) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            } else if (state.results.catalogItems.isEmpty() && state.results.userItems.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    ) {
                        Text(
                            text = "Aucun résultat pour \"${state.query}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalStrakkColors.current.textSecondary,
                        )
                    }
                }
            } else {
                items(
                    items = state.results.catalogItems,
                    key = { "catalog-${it.id}" },
                ) { item ->
                    CatalogItemRow(
                        item = item,
                        isExpanded = expandedCatalogItemId == item.id,
                        onTap = {
                            expandedCatalogItemId = if (expandedCatalogItemId == item.id) null else item.id
                            expandedUserItemName = null
                        },
                        onConfirm = { entry ->
                            onConfirm(entry)
                            expandedCatalogItemId = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .animateItem(),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// =============================================================================
// Frequent item row (expandable)
// =============================================================================

@Composable
private fun FrequentItemRow(
    item: FrequentItem,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onConfirm: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded) LocalStrakkColors.current.surface2 else MaterialTheme.colorScheme.surface,
        modifier = modifier.animateContentSize(animationSpec = tween(200)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name ?: item.normalizedName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                append("${item.protein.toInt()}g prot")
                            }
                            withStyle(SpanStyle(color = LocalStrakkColors.current.textSecondary)) {
                                append(" · ${item.calories.toInt()} kcal")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "🕒",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)),
            ) {
                var gramsInput by rememberSaveable { mutableStateOf("100") }
                ItemQuantityStepper(
                    gramsInput = gramsInput,
                    onGramsChange = { gramsInput = it },
                    onConfirm = {
                        val grams = gramsInput.toDoubleOrNull() ?: 100.0
                        val factor = grams / 100.0
                        val entry = MealEntry(
                            id = "",
                            logDate = "",
                            name = item.name ?: item.normalizedName,
                            protein = item.protein * factor,
                            calories = item.calories * factor,
                            fat = item.fat?.times(factor),
                            carbs = item.carbs?.times(factor),
                            source = com.strakk.shared.domain.model.EntrySource.Frequent,
                            createdAt = "",
                            quantity = "${gramsInput}g",
                        )
                        onConfirm(entry)
                    },
                )
            }
        }
    }
}

// =============================================================================
// Catalog item row (expandable)
// =============================================================================

@Composable
private fun CatalogItemRow(
    item: FoodCatalogItem,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onConfirm: (MealEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded) LocalStrakkColors.current.surface2 else MaterialTheme.colorScheme.surface,
        modifier = modifier.animateContentSize(animationSpec = tween(200)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    item.brand?.let { brand ->
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.labelSmall,
                            color = LocalStrakkColors.current.textSecondary,
                        )
                    }
                }
                item.nutriscore?.firstOrNull()?.let { grade ->
                    NutriscoreBadge(grade = grade)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = LocalStrakkColors.current.calories)) {
                        append("${item.calories.toInt()} kcal")
                    }
                    withStyle(SpanStyle(color = LocalStrakkColors.current.textTertiary)) { append(" · ") }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("${item.protein.toInt()} g prot")
                    }
                    item.fat?.let {
                        withStyle(SpanStyle(color = LocalStrakkColors.current.textTertiary)) { append(" · ") }
                        withStyle(SpanStyle(color = LocalStrakkColors.current.accentYellow)) {
                            append("${it.toInt()} g lip")
                        }
                    }
                    item.carbs?.let {
                        withStyle(SpanStyle(color = LocalStrakkColors.current.textTertiary)) { append(" · ") }
                        withStyle(SpanStyle(color = LocalStrakkColors.current.accentIndigo)) {
                            append("${it.toInt()} g gluc")
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "⚖︎ valeurs pour 100 g",
                style = MaterialTheme.typography.labelSmall,
                color = LocalStrakkColors.current.textTertiary,
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)),
            ) {
                var gramsInput by rememberSaveable { mutableStateOf(item.defaultPortionGrams.toInt().toString()) }
                ItemQuantityStepper(
                    gramsInput = gramsInput,
                    onGramsChange = { gramsInput = it },
                    onConfirm = {
                        val grams = gramsInput.toDoubleOrNull() ?: item.defaultPortionGrams
                        val factor = grams / 100.0
                        val entry = MealEntry(
                            id = "",
                            logDate = "",
                            name = item.name,
                            protein = item.protein * factor,
                            calories = item.calories * factor,
                            fat = item.fat?.times(factor),
                            carbs = item.carbs?.times(factor),
                            source = com.strakk.shared.domain.model.EntrySource.Search,
                            createdAt = "",
                            quantity = "${gramsInput}g",
                        )
                        onConfirm(entry)
                    },
                )
            }
        }
    }
}

// =============================================================================
// Nutri-Score badge (small a..e color chip)
// =============================================================================

@Composable
private fun NutriscoreBadge(grade: Char, modifier: Modifier = Modifier) {
    val color = when (grade) {
        'a' -> androidx.compose.ui.graphics.Color(0xFF1F8E3D)
        'b' -> androidx.compose.ui.graphics.Color(0xFF85BB2F)
        'c' -> androidx.compose.ui.graphics.Color(0xFFF1C232)
        'd' -> androidx.compose.ui.graphics.Color(0xFFE67E22)
        'e' -> androidx.compose.ui.graphics.Color(0xFFC0392B)
        else -> LocalStrakkColors.current.textTertiary
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color,
        modifier = modifier.size(width = 22.dp, height = 22.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = grade.uppercaseChar().toString(),
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White,
            )
        }
    }
}

// =============================================================================
// Stepper inline
// =============================================================================

@Composable
private fun ItemQuantityStepper(
    gramsInput: String,
    onGramsChange: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grams = gramsInput.toIntOrNull() ?: 100

    Column(modifier = modifier.padding(top = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = {
                    val newVal = (grams - 10).coerceAtLeast(10)
                    onGramsChange(newVal.toString())
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "−",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .size(width = 80.dp, height = 40.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "${gramsInput}g",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Surface(
                onClick = {
                    val newVal = (grams + 10).coerceAtMost(2000)
                    onGramsChange(newVal.toString())
                },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.height(40.dp),
            ) {
                Text("Ajouter", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// =============================================================================
// Section overline
// =============================================================================

@Composable
private fun SectionOverline(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = LocalStrakkColors.current.textSecondary,
        letterSpacing = androidx.compose.ui.unit.TextUnit(0.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = modifier,
    )
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun SearchFoodScreenPreview() {
    StrakkTheme {
        SearchFoodScreen(
            uiState = SearchFoodUiState.Ready(
                query = "",
                results = SearchResults(
                    userItems = listOf(
                        FrequentItem(
                            normalizedName = "yaourt nature",
                            name = "Yaourt nature",
                            protein = 5.5,
                            calories = 60.0,
                            fat = 3.0,
                            carbs = 4.5,
                            quantity = "125g",
                            occurrences = 12,
                        ),
                        FrequentItem(
                            normalizedName = "banane",
                            name = "Banane",
                            protein = 1.2,
                            calories = 90.0,
                            fat = 0.3,
                            carbs = 23.0,
                            quantity = null,
                            occurrences = 8,
                        ),
                    ),
                    catalogItems = emptyList(),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onEvent = {},
            onNavigateBack = {},
            onConfirm = {},
        )
    }
}
