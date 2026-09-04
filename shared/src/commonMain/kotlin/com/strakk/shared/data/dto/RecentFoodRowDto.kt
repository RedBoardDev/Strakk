package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Result row of the `recent_foods_v1` RPC. */
@Serializable
internal data class RecentFoodRowDto(
    @SerialName("name_normalized") val nameNormalized: String,
    val name: String?,
    val protein: Double = 0.0,
    val calories: Double = 0.0,
    val fat: Double? = null,
    val carbs: Double? = null,
    val quantity: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String,
)
