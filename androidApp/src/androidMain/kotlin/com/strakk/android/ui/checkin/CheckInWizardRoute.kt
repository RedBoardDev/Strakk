package com.strakk.android.ui.checkin

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.shared.presentation.checkin.CheckInWizardEffect
import com.strakk.shared.presentation.checkin.CheckInWizardViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Wires [CheckInWizardViewModel] to [CheckInWizardScreen].
 * [checkInId] is null for create mode, non-null for edit mode.
 */
@Composable
fun CheckInWizardRoute(
    checkInId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CheckInWizardViewModel = koinViewModel { parametersOf(checkInId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CheckInWizardEffect.NavigateBack -> onNavigateBack()
                is CheckInWizardEffect.NavigateToDetail -> onNavigateToDetail(effect.checkInId)
                is CheckInWizardEffect.ShowError -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    CheckInWizardScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbar = snackbar,
        modifier = modifier,
    )
}
