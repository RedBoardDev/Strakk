package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.CheckInRepository

class GetCheckInPhotoUrlUseCase(
    private val checkInRepository: CheckInRepository,
) {
    suspend operator fun invoke(storagePath: String): Result<String> = runSuspendCatching {
        checkInRepository.getPhotoUrl(storagePath)
    }
}
