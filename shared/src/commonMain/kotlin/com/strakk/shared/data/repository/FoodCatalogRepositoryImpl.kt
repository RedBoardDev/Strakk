package com.strakk.shared.data.repository

import com.strakk.shared.data.datasource.OffLiveSearchDataSource
import com.strakk.shared.data.dto.FoodCatalogItemDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.repository.FoodCatalogRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val LOG_TAG = "FoodCatalog"

internal class FoodCatalogRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val offLive: OffLiveSearchDataSource,
    private val logger: Logger,
) : FoodCatalogRepository {

    override suspend fun search(query: String, limit: Int): List<FoodCatalogItem> = coroutineScope {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@coroutineScope emptyList()

        logger.d(LOG_TAG, "search(\"$trimmed\", limit=$limit) — fan-out")

        val localDeferred = async {
            try {
                runRpc(trimmed, limit).also {
                    logger.d(LOG_TAG, "  local RPC → ${it.size} hit(s)")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                logger.e(LOG_TAG, "  local RPC FAILED: ${e.message}", e)
                emptyList()
            }
        }
        val liveDeferred = async {
            if (trimmed.length < 2) return@async emptyList()
            val live = offLive.search(trimmed, limit)
            logger.d(LOG_TAG, "  live OFF → ${live.size} hit(s)")
            live
        }

        val local = localDeferred.await()
        val live = liveDeferred.await()
        val merged = merge(local, live, limit)
        logger.d(LOG_TAG, "search(\"$trimmed\") → ${merged.size} merged result(s)")
        merged.map { it.toDomain() }
    }

    private suspend fun runRpc(query: String, limit: Int): List<FoodCatalogItemDto> {
        val params = buildJsonObject {
            put("q", query)
            put("lim", limit)
        }
        return supabaseClient.postgrest
            .rpc("search_food_catalog", params)
            .decodeList<FoodCatalogItemDto>()
    }

    /**
     * Order: branded OFF hits first (the user typed text — usually they're
     * looking for a specific product), then generic CIQUAL below as a safety
     * net. Dedup on barcode then on (name, brand) to avoid showing the same
     * thing twice when an OFF item was already cached in the local table.
     */
    private fun merge(
        local: List<FoodCatalogItemDto>,
        live: List<FoodCatalogItemDto>,
        limit: Int,
    ): List<FoodCatalogItemDto> {
        if (live.isEmpty()) return local
        val seenBarcodes = mutableSetOf<String>()
        val seenLabels = mutableSetOf<String>()
        val ordered = (live + local).filter { hit ->
            val barcode = hit.barcode
            if (barcode != null && !seenBarcodes.add(barcode)) return@filter false
            seenLabels.add(dedupKey(hit))
        }
        return ordered.take(limit)
    }

    private fun dedupKey(it: FoodCatalogItemDto): String {
        val n = it.name.lowercase().trim()
        val b = (it.brand ?: "").lowercase().trim()
        return "$n|$b"
    }
}
