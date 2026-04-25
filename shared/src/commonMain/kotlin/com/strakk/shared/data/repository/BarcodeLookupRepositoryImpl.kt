package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.OpenFoodFactsDto
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.BarcodeLookupRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlin.coroutines.cancellation.CancellationException

private const val LOG_TAG = "BarcodeLookup"
private const val OFF_BASE_URL = "https://world.openfoodfacts.org/api/v2/product"

/**
 * Open Food Facts implementation of [BarcodeLookupRepository].
 *
 * Returns null on any failure (unknown barcode, network error, parse error)
 * so the caller can treat the case as "not found" without catching.
 */
internal class BarcodeLookupRepositoryImpl(
    private val httpClient: HttpClient,
    private val logger: Logger,
) : BarcodeLookupRepository {

    override suspend fun lookup(barcode: String): MealEntry? {
        val dto = try {
            httpClient.get("$OFF_BASE_URL/$barcode.json").body<OpenFoodFactsDto>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "Open Food Facts request failed for barcode=$barcode", e)
            return null
        }

        if (dto.status == 0 || dto.product == null) return null

        val product = dto.product
        val nutriments = product.nutriments

        return MealEntry(
            id = "",
            logDate = "",
            name = product.productName.takeIf { it.isNotBlank() },
            protein = nutriments.proteins100g,
            calories = nutriments.energyKcal100g,
            fat = nutriments.fat100g,
            carbs = nutriments.carbohydrates100g,
            source = EntrySource.Barcode,
            createdAt = "",
        )
    }
}
