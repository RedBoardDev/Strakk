package com.strakk.shared.presentation.checkin

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.usecase.GetCheckInStatsUseCase
import com.strakk.shared.domain.usecase.GetHevyApiKeyUseCase
import com.strakk.shared.domain.usecase.ObserveProfileUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CheckInStatsViewModel(
    private val getCheckInStats: GetCheckInStatsUseCase,
    private val observeProfile: ObserveProfileUseCase,
    private val getHevyApiKey: GetHevyApiKeyUseCase,
) : MviViewModel<CheckInStatsUiState, CheckInStatsEvent, CheckInStatsEffect>(CheckInStatsUiState.Loading) {

    // Last-known values kept for synchronous period switches without re-fetching.
    private var cachedSeries: List<CheckInSeriesPoint> = emptyList()
    private var cachedProfile: UserProfile? = null
    private var cachedHasHevyKey: Boolean = false

    init { observe() }

    override fun onEvent(event: CheckInStatsEvent) {
        when (event) {
            is CheckInStatsEvent.OnPeriodSelected -> updatePeriod(event.period)
        }
    }

    // -------------------------------------------------------------------------
    // Observation
    // -------------------------------------------------------------------------

    private fun observe() {
        viewModelScope.launch {
            cachedHasHevyKey = getHevyApiKey().getOrNull() != null

            combine(
                getCheckInStats(),
                observeProfile(),
            ) { allSeries, profile ->
                cachedSeries = allSeries
                cachedProfile = profile
                val period = (uiState.value as? CheckInStatsUiState.Ready)?.selectedPeriod
                    ?: StatsPeriod.TwelveWeeks
                buildReady(allSeries = allSeries, profile = profile, period = period)
            }.collect { state ->
                setState { state }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Period switch — rebuilds synchronously from cached data
    // -------------------------------------------------------------------------

    private fun updatePeriod(period: StatsPeriod) {
        if (uiState.value !is CheckInStatsUiState.Ready) return
        setState {
            buildReady(
                allSeries = cachedSeries,
                profile = cachedProfile,
                period = period,
            )
        }
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    private fun buildReady(
        allSeries: List<CheckInSeriesPoint>,
        profile: UserProfile?,
        period: StatsPeriod,
    ): CheckInStatsUiState.Ready {
        val filtered = filterByPeriod(allSeries, period)
        return CheckInStatsUiState.Ready(
            selectedPeriod = period,
            allSeries = allSeries,
            overview = buildOverview(filtered, profile),
            weightSeries = buildWeightSeries(filtered),
            bodySnapshot = buildBodySnapshot(filtered),
            training = buildTrainingStats(filtered),
            nutrition = buildNutritionStats(filtered, profile),
            regularity = buildRegularity(allSeries, period),
        )
    }

    // -------------------------------------------------------------------------
    // Overview cards
    // -------------------------------------------------------------------------

    private fun buildOverview(filtered: List<CheckInSeriesPoint>, profile: UserProfile?): OverviewCards {
        val withTraining = filtered.filter { it.trainingSessions != null && it.trainingSessions > 0 }
        val avgVolume = withTraining.mapNotNull { it.trainingVolumeKg }
            .takeIf { it.isNotEmpty() }?.average()
        val avgSessions = withTraining.mapNotNull { it.trainingSessions }
            .takeIf { it.isNotEmpty() }?.average()

        val compliancePct = computeCompliancePct(filtered, profile)

        return OverviewCards(
            weightKg = filtered.lastOrNull()?.weight,
            weightTrend = computeWeightTrend(filtered),
            avgWeeklyVolumeKg = avgVolume,
            avgSessionsPerWeek = avgSessions,
            nutritionCompliancePct = compliancePct,
        )
    }

    private fun computeCompliancePct(filtered: List<CheckInSeriesPoint>, profile: UserProfile?): Int? {
        if (profile == null) return null
        val withNutrition = filtered.filter { (it.nutritionDays ?: 0) > 0 }
        if (withNutrition.isEmpty()) return null

        val percentages = mutableListOf<Int>()
        val avgCal = withNutrition.mapNotNull { it.avgCalories }.average()
        val avgProt = withNutrition.mapNotNull { it.avgProtein }.average()
        val avgCarbs = withNutrition.mapNotNull { it.avgCarbs }.average()
        val avgFat = withNutrition.mapNotNull { it.avgFat }.average()

        profile.calorieGoal?.let { if (it > 0) percentages.add(((avgCal / it) * 100).toInt().coerceAtMost(100)) }
        profile.proteinGoal?.let { if (it > 0) percentages.add(((avgProt / it) * 100).toInt().coerceAtMost(100)) }
        profile.carbGoal?.let { if (it > 0) percentages.add(((avgCarbs / it) * 100).toInt().coerceAtMost(100)) }
        profile.fatGoal?.let { if (it > 0) percentages.add(((avgFat / it) * 100).toInt().coerceAtMost(100)) }

        return if (percentages.isNotEmpty()) percentages.average().toInt() else null
    }

    // -------------------------------------------------------------------------
    // Weight series & trend
    // -------------------------------------------------------------------------

    private fun buildWeightSeries(filtered: List<CheckInSeriesPoint>): List<WeightPoint> =
        filtered.mapNotNull { point ->
            point.weight?.let { WeightPoint(weekLabel = point.weekLabel, weightKg = it) }
        }

    private fun computeWeightTrend(filtered: List<CheckInSeriesPoint>): TrendInfo? {
        val weights = filtered.mapNotNull { it.weight }
        if (weights.size < 2) return null
        return TrendInfo(
            delta = weights.last() - weights.first(),
            weeks = weights.size,
        )
    }

    // -------------------------------------------------------------------------
    // Body snapshot (latest check-in measurements)
    // -------------------------------------------------------------------------

    private fun buildBodySnapshot(filtered: List<CheckInSeriesPoint>): BodySnapshot? {
        val latest = filtered.lastOrNull() ?: return null
        val measurements = CheckInMeasurements(
            weight = latest.weight,
            shoulders = latest.shoulders,
            chest = latest.chest,
            armLeft = latest.armLeft,
            armRight = latest.armRight,
            waist = latest.waist,
            hips = latest.hips,
            thighLeft = latest.thighLeft,
            thighRight = latest.thighRight,
        )
        return BodySnapshot(
            measurements = measurements,
            weekLabel = latest.weekLabel,
        )
    }

    private fun buildTrainingStats(filtered: List<CheckInSeriesPoint>): TrainingStats? {
        val withTraining = filtered.filter { it.trainingSessions != null && it.trainingSessions > 0 }
        if (withTraining.isEmpty()) return null

        val volumeSeries = withTraining.map { it.weekLabel to (it.trainingVolumeKg ?: 0.0) }
        val sessionsSeries = withTraining.map { it.weekLabel to (it.trainingSessions ?: 0) }
        val totalVolume = withTraining.sumOf { it.trainingVolumeKg ?: 0.0 }
        val totalSessions = withTraining.sumOf { it.trainingSessions ?: 0 }
        val totalDuration = withTraining.sumOf { it.trainingDurationMin ?: 0 }

        return TrainingStats(
            volumeSeries = volumeSeries,
            sessionsSeries = sessionsSeries,
            totalVolumeKg = totalVolume,
            totalSessions = totalSessions,
            totalDurationMin = totalDuration,
        )
    }

    // -------------------------------------------------------------------------
    // Nutrition stats
    //
    // Per-period averages are not available without a dedicated use case
    // returning an aggregated NutritionSummary per period. Goals from the
    // profile are exposed with avg = 0.0 so the UI can render targets.
    // Once an aggregated use case exists, wire it into observe() and replace
    // the zero averages with real values.
    // -------------------------------------------------------------------------

    @Suppress("CyclomaticComplexMethod")
    private fun buildNutritionStats(filtered: List<CheckInSeriesPoint>, profile: UserProfile?): NutritionStats? {
        if (profile == null) return null
        val withNutrition = filtered.filter { (it.nutritionDays ?: 0) > 0 }

        fun avg(selector: (CheckInSeriesPoint) -> Double?): Double =
            withNutrition.mapNotNull(selector).let { if (it.isNotEmpty()) it.average() else 0.0 }

        val avgCal = avg { it.avgCalories }
        val avgProt = avg { it.avgProtein }
        val avgCarbs = avg { it.avgCarbs }
        val avgFat = avg { it.avgFat }
        val avgWater = withNutrition.mapNotNull { it.avgWater }
            .takeIf { it.isNotEmpty() }?.average()?.toInt()

        fun pct(value: Double, goal: Int?): Int =
            if (goal != null && goal > 0) ((value / goal) * 100).toInt().coerceAtMost(100) else 0

        val macros = buildList<MacroCompliance> {
            profile.calorieGoal?.let { add(MacroCompliance("calories", avgCal, it.toDouble(), pct(avgCal, it))) }
            profile.proteinGoal?.let { add(MacroCompliance("protein", avgProt, it.toDouble(), pct(avgProt, it))) }
            profile.carbGoal?.let { add(MacroCompliance("carbs", avgCarbs, it.toDouble(), pct(avgCarbs, it))) }
            profile.fatGoal?.let { add(MacroCompliance("fat", avgFat, it.toDouble(), pct(avgFat, it))) }
        }
        if (macros.isEmpty() && profile.waterGoal == null) return null
        return NutritionStats(
            macros = macros,
            avgWater = avgWater,
            waterGoal = profile.waterGoal,
        )
    }

    // -------------------------------------------------------------------------
    // Regularity
    // -------------------------------------------------------------------------

    private fun buildRegularity(allSeries: List<CheckInSeriesPoint>, period: StatsPeriod): RegularityInfo {
        val totalWeeks = when (period) {
            StatsPeriod.FourWeeks -> 4
            StatsPeriod.TwelveWeeks -> 12
            StatsPeriod.All -> maxOf(allSeries.size, 1)
        }
        val count = minOf(allSeries.size, totalWeeks)
        val percentage = if (totalWeeks > 0) (count * 100) / totalWeeks else 0
        return RegularityInfo(
            checkInCount = count,
            totalWeeks = totalWeeks,
            percentage = percentage,
        )
    }

    // -------------------------------------------------------------------------
    // Period filter
    // -------------------------------------------------------------------------

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
}
