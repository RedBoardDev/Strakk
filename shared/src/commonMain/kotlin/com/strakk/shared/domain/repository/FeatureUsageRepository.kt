package com.strakk.shared.domain.repository

import kotlinx.datetime.Instant

interface FeatureUsageRepository {
    suspend fun countUsage(featureKey: String, since: Instant): Int
}
