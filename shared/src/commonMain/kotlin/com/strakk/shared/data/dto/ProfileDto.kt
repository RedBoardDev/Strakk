package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the `profiles` table.
 *
 * Field names match the Supabase column names exactly via [@SerialName].
 * Nullable columns reflect optional goals.
 */
@Serializable
internal data class ProfileDto(
    val id: String,
    @SerialName("protein_goal") val proteinGoal: Int?,
    @SerialName("calorie_goal") val calorieGoal: Int?,
    @SerialName("water_goal") val waterGoal: Int?,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
