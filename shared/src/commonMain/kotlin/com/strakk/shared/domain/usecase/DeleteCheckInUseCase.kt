package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.CheckInRepository

class DeleteCheckInUseCase(
    private val checkInRepository: CheckInRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = runSuspendCatching {
        checkInRepository.deleteCheckIn(id)
    }
}
