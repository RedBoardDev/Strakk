package com.strakk.androidApp.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.shared.domain.model.Session
import com.strakk.shared.domain.model.SessionId
import com.strakk.shared.presentation.session.SessionListEffect
import com.strakk.shared.presentation.session.SessionListEvent
import com.strakk.shared.presentation.session.SessionListUiState
import com.strakk.shared.presentation.session.SessionListViewModel
import org.koin.compose.viewmodel.koinViewModel

// =============================================================================
// Layer 1: Route (stateful) — DI, state collection, effects
// =============================================================================

/**
 * Route composable — the only layer that knows about DI and navigation.
 *
 * Responsibilities:
 * - Obtain ViewModel via koinViewModel()
 * - Collect StateFlow with collectAsStateWithLifecycle()
 * - Handle one-shot effects via LaunchedEffect
 * - Delegate rendering to the stateless Screen
 */
@Composable
fun SessionListRoute(
    onNavigateToDetail: (SessionId) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SessionListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot effects — navigation, snackbar, etc.
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SessionListEffect.NavigateToDetail -> {
                    onNavigateToDetail(effect.sessionId)
                }
                is SessionListEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = effect.message)
                }
                is SessionListEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    SessionListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        modifier = Modifier,
    )
}

// =============================================================================
// Layer 2: Screen (stateless) — receives state + callbacks
// =============================================================================

/**
 * Screen composable — stateless, receives everything it needs.
 *
 * Responsibilities:
 * - Layout and scaffold setup
 * - Dispatch events to the ViewModel via callbacks
 * - Modifier is ALWAYS the last parameter
 *
 * This is the level where @Preview is most useful.
 */
@Composable
fun SessionListScreen(
    uiState: SessionListUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (SessionListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { contentPadding ->
        when (uiState) {
            is SessionListUiState.Loading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                )
            }
            is SessionListUiState.Success -> {
                SessionListContent(
                    sessions = uiState.sessions,
                    onSessionClick = { sessionId ->
                        onEvent(SessionListEvent.OnSessionClick(sessionId = sessionId))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                )
            }
            is SessionListUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                )
            }
        }
    }
}

// =============================================================================
// Layer 3: Content (leaf composables) — render sub-state, emit callbacks
// =============================================================================

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SessionListContent(
    sessions: List<Session>,
    onSessionClick: (SessionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        items(
            items = sessions,
            key = { it.id.value },
        ) { session ->
            SessionCard(
                session = session,
                onClick = { onSessionClick(session.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${session.exerciseCount} exercises",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// =============================================================================
// Previews — with fake data
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun SessionListScreenLoadingPreview() {
    SessionListScreen(
        uiState = SessionListUiState.Loading,
        snackbarHostState = SnackbarHostState(),
        onEvent = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SessionListScreenSuccessPreview() {
    SessionListScreen(
        uiState = SessionListUiState.Success(
            sessions = listOf(
                Session(
                    id = SessionId(value = "1"),
                    name = "Push Day",
                    exerciseCount = 5,
                ),
                Session(
                    id = SessionId(value = "2"),
                    name = "Pull Day",
                    exerciseCount = 4,
                ),
                Session(
                    id = SessionId(value = "3"),
                    name = "Leg Day",
                    exerciseCount = 6,
                ),
            ),
        ),
        snackbarHostState = SnackbarHostState(),
        onEvent = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SessionListScreenErrorPreview() {
    SessionListScreen(
        uiState = SessionListUiState.Error(
            message = "Failed to load sessions. Check your connection.",
        ),
        snackbarHostState = SnackbarHostState(),
        onEvent = {},
    )
}
