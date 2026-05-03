package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeatureUsageCountDto(
    val id: String,
    @SerialName("feature_key") val featureKey: String,
)
