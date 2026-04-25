package com.strakk.android.ui.home.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.presentation.meal.MealDraftEffect
import com.strakk.shared.presentation.meal.MealDraftEvent
import com.strakk.shared.presentation.meal.MealDraftUiState
import com.strakk.shared.presentation.meal.MealDraftViewModel
import org.koin.compose.viewmodel.koinViewModel

// =============================================================================
// Route
// =============================================================================

@Composable
fun MealReviewRoute(
    onNavigateBack: () -> Unit,
    onCommitted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MealDraftViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MealDraftEffect.Committed -> {
                    snackbarHostState.showSnackbar("Repas enregistré")
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
        onCommit = { viewModel.onEvent(MealDraftEvent.Commit) },
        modifier = modifier,
    )
}

// =============================================================================
// Screen
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealReviewScreen(
    uiState: MealDraftUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Revue du repas") },
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
        bottomBar = {
            if (uiState is MealDraftUiState.Editing) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onCommit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .height(52.dp),
                    ) {
                        Text("Valider le repas")
                    }
                }
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
                        text = "Aucun repas en cours",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalStrakkColors.current.textSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                is MealDraftUiState.Editing -> {
                    ReviewContent(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewContent(
    state: MealDraftUiState.Editing,
    modifier: Modifier = Modifier,
) {
    val resolvedItems = state.draft.items.filterIsInstance<DraftItem.Resolved>()
    val totalItems = resolvedItems.size

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$totalItems items analysés. Modifiez avant de valider.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalStrakkColors.current.textSecondary,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(
            items = resolvedItems,
            key = { it.id },
        ) { item ->
            ReviewItemCard(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ReviewItemCard(
    item: DraftItem.Resolved,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = item.entry.name ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("${item.entry.protein.toInt()}g prot")
                        }
                        withStyle(SpanStyle(color = LocalStrakkColors.current.textSecondary)) {
                            append(" · ${item.entry.calories.toInt()} kcal")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            // Breakdown sub-items if present
            item.entry.breakdown?.let { breakdown ->
                if (breakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    breakdown.forEach { sub ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, top = 2.dp),
                        ) {
                            Text(
                                text = "• ${sub.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalStrakkColors.current.textSecondary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${sub.protein.toInt()}g / ${sub.calories.toInt()} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalStrakkColors.current.textTertiary,
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun MealReviewScreenPreview() {
    StrakkTheme {
        MealReviewScreen(
            uiState = MealDraftUiState.Empty,
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onCommit = {},
        )
    }
}
