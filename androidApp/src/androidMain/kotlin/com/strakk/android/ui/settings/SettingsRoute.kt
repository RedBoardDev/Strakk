package com.strakk.android.ui.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.paywall.PaywallRoute
import com.strakk.shared.presentation.settings.SettingsEffect
import com.strakk.shared.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wires [SettingsViewModel] to [SettingsScreen].
 *
 * Routes are the only place where ViewModels are instantiated — keeps screen
 * composables pure and preview-friendly.
 */
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowError -> snackbar.showSnackbar(effect.message)
                is SettingsEffect.NavigateToPaywall -> showPaywall = true
                is SettingsEffect.ShowToast -> snackbar.showSnackbar(effect.message)
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

    SettingsScreen(
        state = state,
        snackbar = snackbar,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
