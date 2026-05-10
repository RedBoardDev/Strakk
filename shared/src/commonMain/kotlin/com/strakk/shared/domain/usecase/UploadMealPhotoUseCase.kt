package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.MealPhotoRepository

/**
 * Uploads a base64-encoded JPEG to Storage and returns the storage path.
 *
 * Wraps [MealPhotoRepository.uploadPhoto] so that presentation-layer
 * code does not depend on the repository directly.
 */
class UploadMealPhotoUseCase(
    private val photoRepository: MealPhotoRepository,
) {
    suspend operator fun invoke(draftId: String, itemId: String, base64: String): Result<String> = runSuspendCatching {
        photoRepository.uploadPhoto(draftId, itemId, base64)
    }
}
