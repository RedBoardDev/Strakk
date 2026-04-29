package com.strakk.shared.presentation.checkin

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.usecase.GetCheckInStatsUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.launch

class CheckInStatsViewModel(
    private val getCheckInStats: GetCheckInStatsUseCase,
) : MviViewModel<CheckInStatsUiState, CheckInStatsEvent, CheckInStatsEffect>(CheckInStatsUiState.Loading) {

    init { observe() }

    override fun onEvent(event: CheckInStatsEvent) {
        when (event) {
            is CheckInStatsEvent.OnPeriodSelected -> updatePeriod(event.period)
        }
    }

    private fun observe() {
        viewModelScope.launch {
            getCheckInStats().collect { allSeries ->
                val period = (uiState.value as? CheckInStatsUiState.Ready)?.selectedPeriod
                    ?: StatsPeriod.TwelveWeeks
                setState { buildReady(allSeries, period) }
            }
        }
    }

    private fun updatePeriod(period: StatsPeriod) {
        val state = uiState.value as? CheckInStatsUiState.Ready ?: return
        setState { buildReady(state.series, period) }
    }

    private fun buildReady(
        allSeries: List<CheckInSeriesPoint>,
        period: StatsPeriod,
    ): CheckInStatsUiState.Ready {
        val filtered = filterByPeriod(allSeries, period)
        return CheckInStatsUiState.Ready(
            selectedPeriod = period,
            series = allSeries,
            filteredSeries = filtered,
            weightTrend = computeTrend(filtered) { it.weight },
            waistTrend = computeTrend(filtered) { it.waist },
            regularity = computeRegularity(allSeries, period),
        )
    }

    private fun filterByPeriod(
        series: List<CheckInSeriesPoint>,
        period: StatsPeriod,
    ): List<CheckInSeriesPoint> {
        if (period == StatsPeriod.All || series.isEmpty()) return series
        val count = when (period) {
            StatsPeriod.FourWeeks -> 4
            StatsPeriod.TwelveWeeks -> 12
            StatsPeriod.All -> series.size
        }
        return series.takeLast(count)
    }

    private fun computeTrend(
        series: List<CheckInSeriesPoint>,
        selector: (CheckInSeriesPoint) -> Double?,
    ): TrendInfo? {
        val values = series.mapNotNull { point -> selector(point)?.let { point to it } }
        if (values.size < 2) return null
        val first = values.first().second
        val last = values.last().second
        return TrendInfo(
            delta = last - first,
            weeks = values.size,
        )
    }

    private fun computeRegularity(
        series: List<CheckInSeriesPoint>,
        period: StatsPeriod,
    ): RegularityInfo {
        val totalWeeks = when (period) {
            StatsPeriod.FourWeeks -> 4
            StatsPeriod.TwelveWeeks -> 12
            StatsPeriod.All -> maxOf(series.size, 1)
        }
        val count = minOf(series.size, totalWeeks)
        val percentage = if (totalWeeks > 0) (count * 100) / totalWeeks else 0
        return RegularityInfo(
            checkInCount = count,
            totalWeeks = totalWeeks,
            percentage = percentage,
        )
    }
}
