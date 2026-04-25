package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the `food_catalog` table.
 */
@Serializable
internal data class FoodCatalogItemDto(
    val id: Long,
    val name: String,
    val protein: Double,
    val calories: Double,
    val fat: Double? = null,
    val carbs: Double? = null,
    @SerialName("default_portion_grams") val defaultPortionGrams: Double,
)
