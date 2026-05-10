package com.strakk.shared.domain.model

/**
 * A meal item after the grounding step: an [AiPrediction] enriched with a
 * [FoodCatalogItem] match, a structured quantity, and computed macros.
 *
 * [isGrounded] is false when the edge function could not find a catalogue match —
 * the item still carries AI-estimated macros via [computedMacros].
 */
data class GroundedMealItem(
    val prediction: AiPrediction,
    /** Matched CIQUAL, USDA, or OFF catalogue entry. Null when not grounded. */
    val catalogMatch: FoodCatalogItem?,
    /** `food_catalog.id` of [catalogMatch], or null when ungrounded. */
    val catalogMatchId: Long?,
    /** Grounding source string: "ciqual", "usda", "off_fr", "off_live". */
    val groundingSource: String?,
    /** Cosine / semantic similarity score returned by the grounding service. */
    val similarity: Double,
    val quantity: StructuredQuantity,
    val computedMacros: MacroValues,
    val isGrounded: Boolean,
    val aiConfidence: Double,
    /** Cooking method chosen by the user during review (post-MVP retention factors). */
    val cookingMethod: CookingMethod? = null,
    /** True when the user has soft-hidden this item during review (grayed out, not removed). */
    val isHidden: Boolean = false,
    /** True for items added manually by the user in the review screen (not from the scan). */
    val isManuallyAdded: Boolean = false,
)
