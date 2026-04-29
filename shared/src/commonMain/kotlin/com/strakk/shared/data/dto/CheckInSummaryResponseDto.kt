package com.strakk.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class CheckInSummaryResponseDto(
    val summary: String,
)
