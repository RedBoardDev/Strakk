package com.strakk.android.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.shared.presentation.auth.AuthFlowEffect
import com.strakk.shared.presentation.auth.AuthFlowViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthFlowRoute(
    modifier: Modifier = Modifier,
    viewModel: AuthFlowViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // One-shot effects — errors shown inline in the screen for now
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthFlowEffect.ShowError -> {
                    // Errors are surfaced via uiState.error fields on each sub-state.
                    // Global errors (e.g. resend failure) could be shown via a Snackbar
                    // in a future iteration. No-op here.
                }
            }
        }
    }

    AuthFlowScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
