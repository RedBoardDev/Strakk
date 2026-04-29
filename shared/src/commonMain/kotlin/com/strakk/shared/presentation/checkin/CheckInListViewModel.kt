package com.strakk.shared.presentation.checkin

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.CheckInQuickStats
import com.strakk.shared.domain.usecase.ObserveCheckInQuickStatsUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInsUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CheckInListViewModel(
    private val observeCheckIns: ObserveCheckInsUseCase,
    private val observeQuickStats: ObserveCheckInQuickStatsUseCase,
) : MviViewModel<CheckInListUiState, CheckInListEvent, CheckInListEffect>(CheckInListUiState.Loading) {

    init { observe() }

    override fun onEvent(event: CheckInListEvent) {
        when (event) {
            CheckInListEvent.OnCreateNew -> emit(CheckInListEffect.NavigateToWizard)
            is CheckInListEvent.OnOpenDetail -> emit(CheckInListEffect.NavigateToDetail(event.id))
            CheckInListEvent.OnOpenStats -> emit(CheckInListEffect.NavigateToStats)
        }
    }

    private fun observe() {
        viewModelScope.launch {
            combine(
                observeCheckIns(),
                observeQuickStats(),
            ) { checkIns, quickStats ->
                CheckInListUiState.Ready(
                    checkIns = checkIns,
                    quickStats = quickStats?.toUi(),
                )
            }.collect { setState { it } }
        }
    }

    private fun CheckInQuickStats.toUi() = QuickStats(
        lastWeight = lastWeight,
        weightDelta = weightDelta,
        lastAvgArm = lastAvgArm,
        armDelta = armDelta,
        lastWaist = lastWaist,
        waistDelta = waistDelta,
    )
}
