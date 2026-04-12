package com.strakk.shared.data.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class QuoteDto(
    val id: Int,
    val quote: String,
    val author: String,
)
