package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.MealDraftRepository
import com.strakk.shared.domain.repository.MealPhotoRepository
import kotlinx.coroutines.flow.first

private const val LOG_TAG = "DiscardMealDraftUseCase"

/**
 * Discards the active draft and clears all persisted draft state.
 *
 * Also deletes any photos that were already uploaded to Supabase Storage
 * ([ActiveMealDraft.uploadedPaths]) to prevent orphan objects in the bucket.
 * Photo deletion is best-effort: errors are logged but do not prevent the
 * discard from completing.
 */
class DiscardMealDraftUseCase(
    private val draftRepository: MealDraftRepository,
    private val photoRepository: MealPhotoRepository,
    private val logger: Logger,
) {
    suspend operator fun invoke(): Result<Unit> =
        runSuspendCatching {
            val draft = draftRepository.observeActiveDraft().first()

            val uploadedPaths = draft?.uploadedPaths?.values?.toList().orEmpty()

            draftRepository.discard()

            if (uploadedPaths.isNotEmpty()) {
                try {
                    photoRepository.deletePhotos(uploadedPaths)
                } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(LOG_TAG, "Failed to clean up ${uploadedPaths.size} draft photos", e)
                }
            }
        }
}
