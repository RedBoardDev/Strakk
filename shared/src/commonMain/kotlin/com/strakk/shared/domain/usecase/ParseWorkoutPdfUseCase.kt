package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.WorkoutProgram
import com.strakk.shared.domain.repository.WorkoutRepository

/**
 * Sends a Base64-encoded PDF to the Edge Function and returns a structured [WorkoutProgram].
 *
 * @return [Result.success] with the [WorkoutProgram], or [Result.failure] on network/parsing errors.
 */
class ParseWorkoutPdfUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(pdfBase64: String): Result<WorkoutProgram> =
        runSuspendCatching {
            workoutRepository.parseWorkoutPdf(pdfBase64)
        }
}
