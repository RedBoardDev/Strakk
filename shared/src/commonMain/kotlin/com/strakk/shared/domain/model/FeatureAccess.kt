package com.strakk.shared.domain.model

sealed interface FeatureAccess {
    data class Granted(val remaining: Int? = null) : FeatureAccess

    data class ProRequired(
        val feature: Feature,
        val metadata: FeatureMetadata,
    ) : FeatureAccess

    data class QuotaExhausted(
        val feature: Feature,
        val metadata: FeatureMetadata,
        val used: Int,
        val limit: Int,
        val period: QuotaPeriod,
    ) : FeatureAccess

    data object RateLimited : FeatureAccess
}
