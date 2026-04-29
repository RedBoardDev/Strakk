package com.strakk.shared.domain.model

data class CheckInListItem(
    val id: String,
    val weekLabel: String,
    val weight: Double?,
    val photoCount: Int,
    val hasAiSummary: Boolean,
    val createdAt: String,
)
