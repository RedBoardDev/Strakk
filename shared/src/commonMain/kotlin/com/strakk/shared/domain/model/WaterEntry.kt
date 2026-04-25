package com.strakk.shared.domain.model

/**
 * A single water intake entry logged for a given day.
 *
 * [amount] is in millilitres.
 * Dates are stored as ISO-8601 strings ("yyyy-MM-dd").
 */
data class WaterEntry(
    val id: String,
    val logDate: String,
    val amount: Int,
    val createdAt: String,
)
