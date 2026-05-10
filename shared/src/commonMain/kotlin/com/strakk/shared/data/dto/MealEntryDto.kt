package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the `meal_entries` table.
 *
 * Field names map to Supabase column names via [@SerialName].
 */
@Serializable
internal data class MealEntryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("log_date") val logDate: String,
    val name: String?,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    val source: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("meal_id") val mealId: String? = null,
    val quantity: String? = null,
    @SerialName("breakdown_json") val breakdownJson: String? = null,
    @SerialName("photo_path") val photoPath: String? = null,
    // V3 grounding fields
    @SerialName("food_catalog_id") val foodCatalogId: Long? = null,
    @SerialName("grounding_source") val groundingSource: String? = null,
    @SerialName("quantity_grams") val quantityGrams: Double? = null,
    @SerialName("cooking_method") val cookingMethod: String? = null,
    @SerialName("is_grounded") val isGrounded: Boolean = false,
    @SerialName("ai_confidence") val aiConfidence: Double? = null,
    @SerialName("corrected_food_id") val correctedFoodId: Long? = null,
    @SerialName("corrected_source") val correctedSource: String? = null,
    @SerialName("corrected_quantity") val correctedQuantity: Double? = null,
)
