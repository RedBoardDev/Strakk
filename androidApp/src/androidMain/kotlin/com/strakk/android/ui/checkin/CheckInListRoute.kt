package com.strakk.android.ui.checkin

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.paywall.FeatureGateSheet
import com.strakk.android.ui.paywall.PaywallRoute
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.presentation.checkin.CheckInListEffect
import com.strakk.shared.presentation.checkin.CheckInListViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wires [CheckInListViewModel] to [CheckInListScreen].
 * Handles feature-gating effects and delegates navigation to the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInListRoute(
    onNavigateToWizard: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckInListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var gatedAccess by remember { mutableStateOf<FeatureAccess?>(null) }
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CheckInListEffect.NavigateToWizard -> onNavigateToWizard()
                is CheckInListEffect.NavigateToDetail -> onNavigateToDetail(effect.id)
                CheckInListEffect.NavigateToStats -> onNavigateToStats()
                is CheckInListEffect.FeatureGated -> gatedAccess = effect.access
            }
        }
    }

    gatedAccess?.let { access ->
        val metadata = when (access) {
            is FeatureAccess.ProRequired -> access.metadata
            is FeatureAccess.QuotaExhausted -> access.metadata
            else -> null
        }
        if (metadata != null) {
            FeatureGateSheet(
                metadata = metadata,
                onDiscoverPro = {
                    showPaywall = true
                    gatedAccess = null
                },
                onDismiss = { gatedAccess = null },
            )
        }
    }

    if (showPaywall) {
        PaywallRoute(
            highlightedFeature = null,
            onDismiss = { showPaywall = false },
        )
    }

    CheckInListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
