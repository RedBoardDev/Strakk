package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AiPredictionDto(
    @SerialName("photo_index") val photoIndex: Int,
    val name: String,
    val unit: String,
    val amount: Double,
)

@Serializable
internal data class ComputedMacrosDto(
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
)

// --- scan-meal (single-call VPS passthrough) ---

@Serializable
internal data class ScanMealRequestDto(
    @SerialName("photo_paths") val photoPaths: List<String>,
    val hint: String? = null,
    @SerialName("is_text_only") val isTextOnly: Boolean = false,
)

@Serializable
internal data class ScanMealResponseDto(
    val predictions: List<AiPredictionDto>,
    val items: List<ScanGroundedItemDto>,
)

@Serializable
internal data class ScanGroundedItemDto(
    val prediction: AiPredictionDto,
    val match: ScanFoodMatchDto? = null,
    val macros: ComputedMacrosDto,
    @SerialName("is_grounded") val isGrounded: Boolean,
    val confidence: Double,
)

@Serializable
internal data class ScanFoodMatchDto(
    val id: Long,
    val source: String,
    val name: String,
    val similarity: Double,
    @SerialName("kcal_per_100g") val kcalPer100g: Double = 0.0,
    @SerialName("protein_per_100g") val proteinPer100g: Double = 0.0,
    @SerialName("fat_per_100g") val fatPer100g: Double? = null,
    @SerialName("carbs_per_100g") val carbsPer100g: Double? = null,
    val density: Double? = null,
    @SerialName("default_portion_grams") val defaultPortionGrams: Double = 100.0,
)
