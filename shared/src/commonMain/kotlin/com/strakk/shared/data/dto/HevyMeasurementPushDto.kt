package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for the `push-checkin-to-hevy` Supabase Edge Function. */
@Serializable
internal data class PushMeasurementsToHevyRequestDto(
    @SerialName("checkin_id") val checkinId: String,
    @SerialName("overwrite") val overwrite: Boolean,
)

/** Response from the `push-checkin-to-hevy` Supabase Edge Function. */
@Serializable
internal data class PushMeasurementsToHevyResponseDto(
    @SerialName("date") val date: String,
    @SerialName("conflict") val conflict: Boolean,
)
