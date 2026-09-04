package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.HevyMeasurementPushResult
import com.strakk.shared.domain.repository.CheckInRepository

/**
 * Pushes a check-in's body measurements to Hevy via the `push-checkin-to-hevy` Edge Function.
 *
 * @return [HevyMeasurementPushResult.Pushed] on success, [HevyMeasurementPushResult.Conflict]
 *   if a Hevy body-measurement entry already existed for that date. [Result.failure] on
 *   network, auth, or paywall errors.
 */
class PushCheckInMeasurementsToHevyUseCase(
    private val checkInRepository: CheckInRepository,
) {
    suspend operator fun invoke(checkInId: String, overwrite: Boolean): Result<HevyMeasurementPushResult> =
        runSuspendCatching {
            checkInRepository.pushMeasurementsToHevy(checkInId, overwrite)
        }
}
