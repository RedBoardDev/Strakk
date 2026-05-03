package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeatureLimitsDto(
    @SerialName("feature_key") val featureKey: String,
    @SerialName("pro_only") val proOnly: Boolean,
    @SerialName("quota_free") val quotaFree: Int,
    @SerialName("quota_pro") val quotaPro: Int,
    @SerialName("quota_period") val quotaPeriod: String,
    @SerialName("rate_limit_max") val rateLimitMax: Int,
    @SerialName("rate_limit_window_s") val rateLimitWindowS: Int,
)
