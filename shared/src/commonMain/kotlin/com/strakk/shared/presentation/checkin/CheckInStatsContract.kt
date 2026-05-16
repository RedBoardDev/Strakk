package com.strakk.shared.presentation.checkin

import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.model.CheckInSeriesPoint

enum class StatsPeriod { FourWeeks, TwelveWeeks, All }

// ---------------------------------------------------------------------------
// Overview cards
// ---------------------------------------------------------------------------

data class TrendInfo(
    val delta: Double,
    val weeks: Int,
)

data class OverviewCards(
    /** Latest weight + delta vs period start (null if no weight data). */
    val weightKg: Double?,
    val weightTrend: TrendInfo?,
    /** Avg weekly volume over the period in kg (null if no Hevy data). */
    val avgWeeklyVolumeKg: Double?,
    /** Avg sessions per week over the period (null if no Hevy data). */
    val avgSessionsPerWeek: Double?,
    /** Nutrition compliance 0–100 averaged across tracked macros, or null if no goals set. */
    val nutritionCompliancePct: Int?,
)

// ---------------------------------------------------------------------------
// Body measurements snapshot
// ---------------------------------------------------------------------------

/** Snapshot from the latest check-in, used for the body silhouette display. */
data class BodySnapshot(
    val measurements: CheckInMeasurements,
    val weekLabel: String,
)

// ---------------------------------------------------------------------------
// Training section (only populated when hasHevyData = true)
// ---------------------------------------------------------------------------

data class TrainingStats(
    val volumeSeries: List<Pair<String, Double>>,
    val sessionsSeries: List<Pair<String, Int>>,
    val totalVolumeKg: Double,
    val totalSessions: Int,
    val totalDurationMin: Int,
)

// ---------------------------------------------------------------------------
// Nutrition section
// ---------------------------------------------------------------------------

data class MacroCompliance(
    val name: String,
    /** Average intake over the period. */
    val avg: Double,
    /** Daily goal (from profile). */
    val goal: Double,
    /** avg / goal × 100, clamped 0–999. */
    val percentage: Int,
)

data class NutritionStats(
    val macros: List<MacroCompliance>,
    /** Avg water intake in ml. */
    val avgWater: Int?,
    /** Water goal in ml, from profile. */
    val waterGoal: Int?,
)

// ---------------------------------------------------------------------------
// Consistency
// ---------------------------------------------------------------------------

data class RegularityInfo(
    val checkInCount: Int,
    val totalWeeks: Int,
    val percentage: Int,
)

// ---------------------------------------------------------------------------
// Weight chart
// ---------------------------------------------------------------------------

data class WeightPoint(
    val weekLabel: String,
    val weightKg: Double,
)

// ---------------------------------------------------------------------------
// Root UI state
// ---------------------------------------------------------------------------

sealed interface CheckInStatsUiState {
    data object Loading : CheckInStatsUiState

    data class Ready(
        val selectedPeriod: StatsPeriod,
        /** Full series (all time), used for period switching without re-fetch. */
        val allSeries: List<CheckInSeriesPoint>,
        val overview: OverviewCards,
        val weightSeries: List<WeightPoint>,
        val bodySnapshot: BodySnapshot?,
        /** Null when no Hevy integration is active or no data for the period. */
        val training: TrainingStats?,
        /** Null when profile goals are not configured. */
        val nutrition: NutritionStats?,
        val regularity: RegularityInfo,
    ) : CheckInStatsUiState
}

// ---------------------------------------------------------------------------
// Events & Effects
// ---------------------------------------------------------------------------

sealed interface CheckInStatsEvent {
    data class OnPeriodSelected(val period: StatsPeriod) : CheckInStatsEvent
}

sealed interface CheckInStatsEffect
