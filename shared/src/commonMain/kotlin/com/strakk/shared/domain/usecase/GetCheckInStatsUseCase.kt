package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.CheckInSeriesPoint
import com.strakk.shared.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow

class GetCheckInStatsUseCase(
    private val checkInRepository: CheckInRepository,
) {
    operator fun invoke(): Flow<List<CheckInSeriesPoint>> =
        checkInRepository.observeCheckInSeries()
}
