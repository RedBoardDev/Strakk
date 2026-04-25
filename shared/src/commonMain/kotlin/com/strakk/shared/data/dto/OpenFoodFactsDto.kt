package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Root response from the Open Food Facts v2 API. */
@Serializable
internal data class OpenFoodFactsDto(
    val status: Int = 0,
    val product: OpenFoodFactsProductDto? = null,
)

/** Product metadata returned by Open Food Facts. */
@Serializable
internal data class OpenFoodFactsProductDto(
    @SerialName("product_name") val productName: String = "",
    val nutriments: OpenFoodFactsNutrimentsDto = OpenFoodFactsNutrimentsDto(),
)

/** Nutritional values per 100g from Open Food Facts. */
@Serializable
internal data class OpenFoodFactsNutrimentsDto(
    @SerialName("proteins_100g") val proteins100g: Double = 0.0,
    @SerialName("energy-kcal_100g") val energyKcal100g: Double = 0.0,
    @SerialName("fat_100g") val fat100g: Double = 0.0,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double = 0.0,
)
