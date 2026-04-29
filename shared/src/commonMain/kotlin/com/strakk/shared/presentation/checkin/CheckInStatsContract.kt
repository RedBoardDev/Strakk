package com.strakk.shared.presentation.checkin

import com.strakk.shared.domain.model.CheckInSeriesPoint

enum class StatsPeriod { FourWeeks, TwelveWeeks, All }

sealed interface CheckInStatsUiState {
    data object Loading : CheckInStatsUiState
    data class Ready(
        val selectedPeriod: StatsPeriod,
        val series: List<CheckInSeriesPoint>,
        val filteredSeries: List<CheckInSeriesPoint>,
        val weightTrend: TrendInfo?,
        val waistTrend: TrendInfo?,
        val regularity: RegularityInfo,
    ) : CheckInStatsUiState
}

data class StatsPoint(
    val weekLabel: String,
    val value: Double,
)

data class TrendInfo(
    val delta: Double,
    val weeks: Int,
)

data class RegularityInfo(
    val checkInCount: Int,
    val totalWeeks: Int,
    val percentage: Int,
)

sealed interface CheckInStatsEvent {
    data class OnPeriodSelected(val period: StatsPeriod) : CheckInStatsEvent
}

sealed interface CheckInStatsEffect
