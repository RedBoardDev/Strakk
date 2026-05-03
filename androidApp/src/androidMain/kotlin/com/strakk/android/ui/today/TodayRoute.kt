package com.strakk.android.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.paywall.PaywallRoute
import com.strakk.shared.presentation.today.TodayEffect
import com.strakk.shared.presentation.today.TodayUiState
import com.strakk.shared.presentation.today.TodayViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayRoute(
    onNavigateToDraft: () -> Unit,
    onNavigateToQuickAdd: () -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TodayEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is TodayEffect.NavigateToPaywall -> showPaywall = true
            }
        }
    }

    if (showPaywall) {
        PaywallRoute(
            onDismiss = { showPaywall = false },
            modifier = modifier,
        )
        return
    }

    Box(modifier = modifier) {
        when (val state = uiState) {
            is TodayUiState.Loading -> TodayLoadingContent(modifier = Modifier.fillMaxSize())
            is TodayUiState.Ready -> TodayContent(
                uiState = state,
                onEvent = viewModel::onEvent,
                onNavigateToDraft = onNavigateToDraft,
                onNavigateToQuickAdd = onNavigateToQuickAdd,
                onDiscardDraft = onDiscardDraft,
                modifier = Modifier.fillMaxSize(),
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TodayLoadingContent(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
