package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Result row of the `recent_meals_v1` RPC. */
@Serializable
internal data class RecentMealRowDto(
    @SerialName("meal_id") val mealId: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,
    val items: JsonElement,
)
