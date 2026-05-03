package com.strakk.android.ui.paywall

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.presentation.paywall.PaywallEffect
import com.strakk.shared.presentation.paywall.PaywallViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PaywallRoute(highlightedFeature: Feature? = null, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: PaywallViewModel = koinViewModel { parametersOf(highlightedFeature) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PaywallEffect.Dismiss -> onDismiss()
                is PaywallEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    PaywallScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
