package com.strakk.shared.domain.model

/**
 * Result of the `scan-meal` edge function: AI predictions alongside
 * fully grounded meal items in a single call.
 */
data class ScanMealResult(
    val predictions: List<AiPrediction>,
    val items: List<GroundedMealItem>,
)
