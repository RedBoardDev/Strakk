package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.AiGoalsResult
import com.strakk.shared.domain.model.CalculateGoalsRequest
import com.strakk.shared.domain.repository.GoalsRepository

class CalculateGoalsUseCase(
    private val goalsRepository: GoalsRepository,
) {
    suspend operator fun invoke(request: CalculateGoalsRequest): Result<AiGoalsResult> =
        runSuspendCatching {
            goalsRepository.calculateGoals(request)
        }
}
