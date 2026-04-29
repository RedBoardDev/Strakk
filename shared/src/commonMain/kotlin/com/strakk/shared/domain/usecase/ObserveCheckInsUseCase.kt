package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.CheckInListItem
import com.strakk.shared.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow

class ObserveCheckInsUseCase(
    private val checkInRepository: CheckInRepository,
) {
    operator fun invoke(): Flow<List<CheckInListItem>> =
        checkInRepository.observeCheckIns()
}
