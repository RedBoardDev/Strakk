package com.strakk.android.ui.checkin

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.shared.presentation.checkin.CheckInDetailEffect
import com.strakk.shared.presentation.checkin.CheckInDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Wires [CheckInDetailViewModel] to [CheckInDetailScreen].
 * [checkInId] identifies the check-in to display.
 */
@Composable
fun CheckInDetailRoute(
    checkInId: String,
    onNavigateBack: () -> Unit,
    onNavigateToWizard: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CheckInDetailViewModel = koinViewModel { parametersOf(checkInId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CheckInDetailEffect.NavigateBack -> onNavigateBack()
                is CheckInDetailEffect.NavigateToWizard -> onNavigateToWizard(effect.checkInId)
                is CheckInDetailEffect.ShowError -> snackbar.showSnackbar(effect.message)
                // Push-to-Hevy has no Android UI yet — these effects are unreachable from this screen.
                CheckInDetailEffect.RequireHevyApiKey,
                is CheckInDetailEffect.HevyPushSucceeded,
                is CheckInDetailEffect.HevyPushConflict,
                -> Unit
            }
        }
    }

    CheckInDetailScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        snackbar = snackbar,
        modifier = modifier,
    )
}
