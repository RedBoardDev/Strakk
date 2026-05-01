package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.AiGoalsResult
import com.strakk.shared.domain.model.CalculateGoalsRequest

interface GoalsRepository {
    suspend fun calculateGoals(request: CalculateGoalsRequest): AiGoalsResult
}
