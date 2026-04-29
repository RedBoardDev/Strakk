package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.CheckInQuickStats
import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCheckInQuickStatsUseCase(
    private val checkInRepository: CheckInRepository,
) {
    operator fun invoke(): Flow<CheckInQuickStats?> =
        checkInRepository.observeCheckInSeries().map(::buildQuickStats)

    private fun buildQuickStats(series: List<CheckInSeriesPoint>): CheckInQuickStats? {
        if (series.isEmpty()) return null

        val latest = series.first()
        val previous = series.getOrNull(1)

        return CheckInQuickStats(
            lastWeight = latest.weight,
            weightDelta = delta(latest.weight, previous?.weight),
            lastAvgArm = avgOf(latest.armLeft, latest.armRight),
            armDelta = delta(
                avgOf(latest.armLeft, latest.armRight),
                avgOf(previous?.armLeft, previous?.armRight),
            ),
            lastWaist = latest.waist,
            waistDelta = delta(latest.waist, previous?.waist),
        )
    }

    private fun delta(current: Double?, previous: Double?): Double? {
        if (current == null || previous == null) return null
        return current - previous
    }

    private fun avgOf(a: Double?, b: Double?): Double? {
        val values = listOfNotNull(a, b)
        if (values.isEmpty()) return null
        return values.sum() / values.size
    }
}
