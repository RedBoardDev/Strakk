package com.strakk.android.ui.checkin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.shared.presentation.checkin.CheckInStatsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wires [CheckInStatsViewModel] to [CheckInStatsScreen].
 */
@Composable
fun CheckInStatsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CheckInStatsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CheckInStatsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}
