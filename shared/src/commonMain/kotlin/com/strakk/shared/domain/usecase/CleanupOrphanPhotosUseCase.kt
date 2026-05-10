package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.repository.MealPhotoRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Best-effort cleanup of uploaded meal photos that have not been attached
 * to a saved meal.
 *
 * Wraps [MealPhotoRepository.deletePhotos] so that presentation-layer
 * code does not depend on the repository directly.
 * Failures are silently swallowed — this is intentionally fire-and-forget.
 */
class CleanupOrphanPhotosUseCase(
    private val photoRepository: MealPhotoRepository,
) {
    suspend operator fun invoke(paths: List<String>) {
        if (paths.isEmpty()) return
        try {
            photoRepository.deletePhotos(paths)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort — orphan cleanup failure is non-critical
        }
    }
}
