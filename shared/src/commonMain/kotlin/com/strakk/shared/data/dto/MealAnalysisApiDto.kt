package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Shared response type (analyze-meal-single + extract-meal-draft items)
// =============================================================================

/**
 * Claude-analyzed entry, returned by both meal-analysis edge functions.
 *
 * Maps to [com.strakk.shared.domain.model.MealEntry] in the domain layer
 * after the client chooses the [source] (PhotoAi / TextAi).
 */
@Serializable
internal data class AnalyzedEntryDto(
    val name: String,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("calories_kcal") val caloriesKcal: Double,
    @SerialName("fat_g") val fatG: Double? = null,
    @SerialName("carbs_g") val carbsG: Double? = null,
    val quantity: String? = null,
    val breakdown: List<BreakdownItemDto>? = null,
)

// =============================================================================
// analyze-meal-single — POST /functions/v1/analyze-meal-single
// =============================================================================

/**
 * Request payload for `analyze-meal-single`.
 *
 * Exactly one of [imageBase64] or [description] must be set, matching [type].
 */
@Serializable
internal data class AnalyzeMealSingleRequestDto(
    val type: String,
    @SerialName("image_base64") val imageBase64: String? = null,
    val hint: String? = null,
    val description: String? = null,
) {
    companion object {
        fun photo(imageBase64: String, hint: String?): AnalyzeMealSingleRequestDto =
            AnalyzeMealSingleRequestDto(
                type = "photo",
                imageBase64 = imageBase64,
                hint = hint?.trim()?.ifBlank { null },
            )

        fun text(description: String): AnalyzeMealSingleRequestDto =
            AnalyzeMealSingleRequestDto(type = "text", description = description.trim())
    }
}

// The 200 response is an [AnalyzedEntryDto] directly (no wrapper).

// =============================================================================
// extract-meal-draft — POST /functions/v1/extract-meal-draft
// =============================================================================

/**
 * Batch request payload.
 *
 * [ExtractDraftItemDto] uses `type` as a discriminator, matching the TypeScript
 * edge function which does not use kotlinx-style polymorphism.  For maximum
 * interop, we model it as a single flat class with nullable fields.
 */
@Serializable
internal data class ExtractMealDraftRequestDto(
    val items: List<ExtractDraftItemDto>,
)

@Serializable
internal data class ExtractDraftItemDto(
    val id: String,
    val type: String, // "photo" or "text"
    @SerialName("photo_path") val photoPath: String? = null,
    val hint: String? = null,
    val description: String? = null,
) {
    companion object {
        fun photo(id: String, photoPath: String, hint: String?): ExtractDraftItemDto =
            ExtractDraftItemDto(
                id = id,
                type = "photo",
                photoPath = photoPath,
                hint = hint?.trim()?.ifBlank { null },
            )

        fun text(id: String, description: String): ExtractDraftItemDto =
            ExtractDraftItemDto(
                id = id,
                type = "text",
                description = description.trim(),
            )
    }
}

@Serializable
internal data class ExtractMealDraftResponseDto(
    val items: List<ExtractedItemDto> = emptyList(),
    val failures: List<ExtractFailureDto> = emptyList(),
)

@Serializable
internal data class ExtractedItemDto(
    val id: String,
    val entry: AnalyzedEntryDto,
)

@Serializable
internal data class ExtractFailureDto(
    val id: String,
    val reason: String,
)
