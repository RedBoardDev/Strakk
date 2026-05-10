package com.strakk.shared.domain.model

/**
 * A quantity expressed in both gram-equivalent and human-readable form.
 *
 * After the grounding step everything is normalised to [grams] so macro
 * computations stay consistent across unit types.
 *
 * [piecesCount] and [pieceWeightGrams] are populated only for [UnitType.Pieces]
 * quantities where the individual piece weight is known.
 */
data class StructuredQuantity(
    /** Gram-equivalent used for all macro computations. */
    val grams: Double,
    /** Human-readable label shown in the UI, e.g. "200g", "2 œufs". */
    val displayLabel: String,
    val unitType: UnitType,
    val piecesCount: Double? = null,
    val pieceWeightGrams: Double? = null,
)
