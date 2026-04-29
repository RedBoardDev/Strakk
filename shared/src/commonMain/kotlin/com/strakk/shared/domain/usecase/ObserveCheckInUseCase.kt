package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow

class ObserveCheckInUseCase(
    private val checkInRepository: CheckInRepository,
) {
    operator fun invoke(id: String): Flow<CheckIn?> =
        checkInRepository.observeCheckIn(id)
}
