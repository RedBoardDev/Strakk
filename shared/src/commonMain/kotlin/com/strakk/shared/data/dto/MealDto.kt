package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the `meals` table.
 *
 * Nested [mealEntries] is populated by a PostgREST embed
 * (`select=*,meal_entries(*)`).
 */
@Serializable
internal data class MealDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("meal_entries") val mealEntries: List<MealEntryDto> = emptyList(),
)
