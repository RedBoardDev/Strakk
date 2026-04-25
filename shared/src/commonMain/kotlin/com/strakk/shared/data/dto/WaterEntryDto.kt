package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the `water_entries` table.
 *
 * Field names map to Supabase column names via [@SerialName].
 */
@Serializable
internal data class WaterEntryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("log_date") val logDate: String,
    val amount: Int,
    @SerialName("created_at") val createdAt: String,
)
