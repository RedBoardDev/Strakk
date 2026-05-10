package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.AiPredictionDto
import com.strakk.shared.data.dto.ScanFoodMatchDto
import com.strakk.shared.data.dto.ScanGroundedItemDto
import com.strakk.shared.domain.model.AiPrediction
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.FoodCatalogSource
import com.strakk.shared.domain.model.GroundedMealItem
import com.strakk.shared.domain.model.MacroValues
import com.strakk.shared.domain.model.StructuredQuantity
import com.strakk.shared.domain.model.UnitType
import kotlin.math.roundToInt

// =============================================================================
// DTO → Domain
// =============================================================================

internal fun AiPredictionDto.toDomain(): AiPrediction = AiPrediction(
    photoIndex = photoIndex,
    name = name,
    unit = unit,
    amount = amount,
)

private const val DECIMAL_PRECISION = 10
private const val DECIMAL_PRECISION_DBL = 10.0

private fun Double.roundToCompact(): String {
    if (this == this.roundToInt().toDouble()) return this.roundToInt().toString()
    val rounded = (this * DECIMAL_PRECISION).roundToInt() / DECIMAL_PRECISION_DBL
    return rounded.toString().trimEnd('0').trimEnd('.')
}

internal fun ScanGroundedItemDto.toDomain(): GroundedMealItem {
    val catalogItem = match?.toCatalogItem()
    val grams = macros.grams
    val originalLabel = "${prediction.amount.roundToCompact()} ${prediction.unit}".trim()
    val displayLabel = "${grams.roundToInt()}g"
    val labelWithOriginal = if (originalLabel.isNotEmpty() && originalLabel != displayLabel) {
        "$displayLabel ($originalLabel)"
    } else {
        displayLabel
    }
    return GroundedMealItem(
        prediction = prediction.toDomain(),
        catalogMatch = catalogItem,
        catalogMatchId = match?.id,
        groundingSource = match?.source,
        similarity = match?.similarity ?: 0.0,
        quantity = StructuredQuantity(
            grams = grams,
            displayLabel = labelWithOriginal,
            unitType = UnitType.Grams,
        ),
        computedMacros = MacroValues(
            kcal = macros.kcal,
            protein = macros.protein,
            fat = macros.fat,
            carbs = macros.carbs,
        ),
        isGrounded = isGrounded,
        aiConfidence = confidence,
    )
}

private fun ScanFoodMatchDto.toCatalogItem(): FoodCatalogItem = FoodCatalogItem(
    id = id,
    source = FoodCatalogSource.fromDbString(source),
    name = name,
    brand = null,
    protein = proteinPer100g,
    calories = kcalPer100g,
    fat = fatPer100g,
    carbs = carbsPer100g,
    defaultPortionGrams = defaultPortionGrams,
    servingLabel = null,
    nutriscore = null,
    novaGroup = null,
    barcode = null,
    imageUrl = null,
)
