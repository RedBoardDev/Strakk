package com.strakk.shared.presentation.checkin

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.CheckInQuickStats
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureAccess
import com.strakk.shared.domain.model.FeatureRegistry
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
            CheckInListEvent.OnUnlockHistory -> handleUnlockHistory()
        }
    }

    private fun handleCreateNew() {
        viewModelScope.launch {
            when (val access = checkFeatureAccess(Feature.AI_WEEKLY_SUMMARY)) {
                is FeatureAccess.Granted -> emit(CheckInListEffect.NavigateToWizard)
                else -> emit(CheckInListEffect.FeatureGated(access))
            }
        }
    }

    private fun handleUnlockHistory() {
        emit(
            CheckInListEffect.FeatureGated(
                FeatureAccess.ProRequired(
                    feature = Feature.UNLIMITED_HISTORY,
                    metadata = FeatureRegistry.get(Feature.UNLIMITED_HISTORY),
                ),
            ),
        )
    }

    private fun observe() {
        viewModelScope.launch {
            combine(
                observeCheckIns(),
                observeQuickStats(),
            ) { page, quickStats ->
                CheckInListUiState.Ready(
                    checkIns = page.items,
                    quickStats = quickStats?.toUi(),
                    hiddenCount = page.hiddenCount,
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
