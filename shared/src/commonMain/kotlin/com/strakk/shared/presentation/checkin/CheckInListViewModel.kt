package com.strakk.shared.presentation.checkin

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.CheckInQuickStats
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.ProFeature
import com.strakk.shared.domain.usecase.CheckFeatureAccessUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInQuickStatsUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInsUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CheckInListViewModel(
    private val observeCheckIns: ObserveCheckInsUseCase,
    private val observeQuickStats: ObserveCheckInQuickStatsUseCase,
    private val checkFeatureAccess: CheckFeatureAccessUseCase,
) : MviViewModel<CheckInListUiState, CheckInListEvent, CheckInListEffect>(CheckInListUiState.Loading) {

    init { observe() }

    override fun onEvent(event: CheckInListEvent) {
        when (event) {
            CheckInListEvent.OnCreateNew -> handleCreateNew()
            is CheckInListEvent.OnOpenDetail -> emit(CheckInListEffect.NavigateToDetail(event.id))
            CheckInListEvent.OnOpenStats -> emit(CheckInListEffect.NavigateToStats)
        }
    }

    private fun handleCreateNew() {
        viewModelScope.launch {
            when (checkFeatureAccess(ProFeature.AI_WEEKLY_SUMMARY)) {
                is FeatureAccess.Granted -> emit(CheckInListEffect.NavigateToWizard)
                is FeatureAccess.Gated -> emit(CheckInListEffect.FeatureGated(ProFeature.AI_WEEKLY_SUMMARY))
            }
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
