package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CheckInPhotoDto(
    val id: String,
    @SerialName("checkin_id") val checkinId: String,
    @SerialName("storage_path") val storagePath: String,
    val position: Int,
    @SerialName("created_at") val createdAt: String,
)
