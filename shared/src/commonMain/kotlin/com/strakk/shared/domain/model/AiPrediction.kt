package com.strakk.shared.domain.model

/**
 * A single food item prediction emitted by the AI identification step.
 *
 * [photoIndex] ties the prediction back to the originating photo when a meal
 * spans multiple photos.
 */
data class AiPrediction(
    val photoIndex: Int,
    /** Canonical descriptive name, e.g. "chicken breast, grilled, skinless". */
    val name: String,
    /** Unit string returned by the AI, e.g. "g", "ml", "pieces". */
    val unit: String,
    val amount: Double,
)
