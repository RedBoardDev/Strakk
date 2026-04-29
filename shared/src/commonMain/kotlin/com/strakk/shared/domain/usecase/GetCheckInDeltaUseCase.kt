package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.repository.CheckInRepository

class GetCheckInDeltaUseCase(
    private val checkInRepository: CheckInRepository,
) {
    suspend operator fun invoke(
        weekLabel: String,
        current: CheckInMeasurements,
    ): Result<CheckInDelta?> = runSuspendCatching {
        val previous = checkInRepository.getPreviousMeasurements(weekLabel)
            ?: return@runSuspendCatching null

        CheckInDelta(
            weight = delta(current.weight, previous.weight),
            shoulders = delta(current.shoulders, previous.shoulders),
            chest = delta(current.chest, previous.chest),
            armLeft = delta(current.armLeft, previous.armLeft),
            armRight = delta(current.armRight, previous.armRight),
            waist = delta(current.waist, previous.waist),
            hips = delta(current.hips, previous.hips),
            thighLeft = delta(current.thighLeft, previous.thighLeft),
            thighRight = delta(current.thighRight, previous.thighRight),
        )
    }

    private fun delta(current: Double?, previous: Double?): Double? {
        if (current == null || previous == null) return null
        return current - previous
    }
}
