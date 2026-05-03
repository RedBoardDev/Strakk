package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.FeatureLimits

interface FeatureLimitsRepository {
    suspend fun getLimits(featureKey: String): FeatureLimits?
    suspend fun getAllLimits(): List<FeatureLimits>
}
