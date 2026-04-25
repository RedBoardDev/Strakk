package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.HevyExportResult
import com.strakk.shared.domain.model.WorkoutSession
import com.strakk.shared.domain.repository.WorkoutRepository

/**
 * Exports a [WorkoutSession] to Hevy as a routine.
 *
 * @return [Result.success] with the [HevyExportResult], or [Result.failure] on network/API errors.
 */
class ExportToHevyUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(session: WorkoutSession, hevyApiKey: String): Result<HevyExportResult> =
        runSuspendCatching {
            workoutRepository.exportSessionToHevy(session, hevyApiKey)
        }
}
