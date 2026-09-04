package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Data Transfer Object for the `favorite_meals` table.
 *
 * [itemsJson] is the JSONB column — decoded client-side into
 * [com.strakk.shared.domain.model.MealTemplateItem]s via the
 * [com.strakk.shared.data.mapper.FavoritesMapper].
 */
@Serializable
internal data class FavoriteMealDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    @SerialName("items_json") val itemsJson: JsonElement,
    @SerialName("source_meal_id") val sourceMealId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
