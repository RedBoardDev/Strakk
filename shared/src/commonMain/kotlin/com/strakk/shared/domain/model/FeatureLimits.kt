package com.strakk.shared.domain.model

data class FeatureLimits(
    val featureKey: String,
    val proOnly: Boolean,
    val quotaFree: Int,
    val quotaPro: Int,
    val quotaPeriod: QuotaPeriod,
    val rateLimitMax: Int,
    val rateLimitWindowSeconds: Int,
)
