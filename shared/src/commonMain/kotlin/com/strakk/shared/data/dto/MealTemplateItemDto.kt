package com.strakk.shared.data.dto

import kotlinx.serialization.Serializable

/**
 * JSONB inner shape used inside `favorite_meals.items_json` and the
 * `recent_meals_v1.items` aggregate.
 */
@Serializable
internal data class MealTemplateItemDto(
    val name: String,
    val protein: Double = 0.0,
    val calories: Double = 0.0,
    val fat: Double? = null,
    val carbs: Double? = null,
    val quantity: String? = null,
)
