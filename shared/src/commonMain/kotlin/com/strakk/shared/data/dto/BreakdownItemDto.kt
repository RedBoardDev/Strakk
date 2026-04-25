package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable form of a [com.strakk.shared.domain.model.BreakdownItem].
 *
 * Stored inside the `breakdown_json` JSONB column on `meal_entries` and
 * echoed back from the `analyze-meal-single` / `extract-meal-draft`
 * edge functions.
 */
@Serializable
internal data class BreakdownItemDto(
    val name: String,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("calories_kcal") val caloriesKcal: Double,
    @SerialName("fat_g") val fatG: Double? = null,
    @SerialName("carbs_g") val carbsG: Double? = null,
    val quantity: String? = null,
)
