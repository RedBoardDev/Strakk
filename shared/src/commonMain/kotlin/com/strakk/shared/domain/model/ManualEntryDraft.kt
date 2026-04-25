package com.strakk.shared.domain.model

/**
 * Form data for a manually-entered meal entry.
 *
 * Validation constraints (enforced by [AddManualEntryUseCase]):
 * - [name]: 1-100 chars.
 * - [protein]: ≥ 0, ≤ 500.
 * - [calories]: ≥ 0, ≤ 5000.
 * - [fat]: ≥ 0, ≤ 500 (optional).
 * - [carbs]: ≥ 0, ≤ 500 (optional).
 * - [quantity]: 0-50 chars (optional).
 */
data class ManualEntryDraft(
    val name: String,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    val quantity: String?,
)
