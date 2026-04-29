package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row returned by the `search_food_catalog(q, lim)` Postgres RPC.
 *
 * The shape mirrors the function's RETURNS TABLE columns. Plain table
 * SELECTs on `food_catalog` are no longer used from the client; ranking
 * and dedup happen server-side.
 */
@Serializable
internal data class FoodCatalogItemDto(
    val id: Long,
    val source: String,
    val name: String,
    val brand: String? = null,
    val protein: Double,
    val calories: Double,
    val fat: Double? = null,
    val carbs: Double? = null,
    @SerialName("default_portion_grams") val defaultPortionGrams: Double,
    @SerialName("serving_label") val servingLabel: String? = null,
    val nutriscore: String? = null,
    @SerialName("nova_group") val novaGroup: Int? = null,
    val barcode: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val rank: Double = 0.0,
)
