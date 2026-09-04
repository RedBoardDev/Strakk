package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the `favorite_foods` table.
 */
@Serializable
internal data class FavoriteFoodDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    @SerialName("name_normalized") val nameNormalized: String,
    val protein: Double = 0.0,
    val calories: Double = 0.0,
    val fat: Double? = null,
    val carbs: Double? = null,
    val quantity: String? = null,
    @SerialName("food_catalog_id") val foodCatalogId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
