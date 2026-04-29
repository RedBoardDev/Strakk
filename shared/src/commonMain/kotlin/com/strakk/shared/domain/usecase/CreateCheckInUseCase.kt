package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInInput
import com.strakk.shared.domain.repository.CheckInRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CreateCheckInUseCase(
    private val checkInRepository: CheckInRepository,
) {
    suspend operator fun invoke(
        input: CheckInInput,
        photos: List<ByteArray>,
    ): Result<CheckIn> = runSuspendCatching {
        val checkIn = checkInRepository.createCheckIn(input)
        if (photos.isNotEmpty()) {
            coroutineScope {
                photos.mapIndexed { index, data ->
                    async { checkInRepository.uploadPhoto(checkIn.id, data, index) }
                }.awaitAll()
            }
        }
        checkIn
    }
}
