package com.strakk.shared.domain.model

data class CheckInsPage(
    val items: List<CheckInListItem>,
    val hiddenCount: Int,
)
