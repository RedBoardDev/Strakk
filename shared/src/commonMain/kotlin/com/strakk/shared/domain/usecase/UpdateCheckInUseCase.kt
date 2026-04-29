package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInInput
import com.strakk.shared.domain.repository.CheckInRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class UpdateCheckInUseCase(
    private val checkInRepository: CheckInRepository,
) {
    suspend operator fun invoke(
        id: String,
        input: CheckInInput,
        newPhotos: List<ByteArray>,
        deletedPhotoIds: List<Pair<String, String>>,
    ): Result<CheckIn> = runSuspendCatching {
        val checkIn = checkInRepository.updateCheckIn(id, input)
        coroutineScope {
            val deleteJobs = deletedPhotoIds.map { (photoId, path) ->
                async { checkInRepository.deletePhoto(photoId, path) }
            }
            val existingCount = checkIn.photos.size - deletedPhotoIds.size
            val uploadJobs = newPhotos.mapIndexed { index, data ->
                async { checkInRepository.uploadPhoto(id, data, existingCount + index) }
            }
            (deleteJobs + uploadJobs).awaitAll()
        }
        checkIn
    }
}
