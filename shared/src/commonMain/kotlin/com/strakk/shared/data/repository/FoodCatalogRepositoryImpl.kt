package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.FoodCatalogItemDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.repository.FoodCatalogRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/**
 * PostgREST-backed implementation of [FoodCatalogRepository].
 *
 * Uses substring matching on the pre-normalized `name_normalized` column,
 * which is indexed with `pg_trgm` for efficient ilike lookups (see
 * `idx_food_catalog_name_trgm` in the init migration).
 */
internal class FoodCatalogRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : FoodCatalogRepository {

    override suspend fun search(query: String, limit: Int): List<FoodCatalogItem> {
        val normalized = normalizeQuery(query)
        if (normalized.isBlank()) return emptyList()

        return supabaseClient
            .from("food_catalog")
            .select(
                columns = io.github.jan.supabase.postgrest.query.Columns.raw(
                    "id,name,protein,calories,fat,carbs,default_portion_grams",
                ),
            ) {
                filter {
                    ilike("name_normalized", "%$normalized%")
                }
                order("name_normalized", Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList<FoodCatalogItemDto>()
            .map { it.toDomain() }
    }

    private fun normalizeQuery(query: String): String =
        query.lowercase()
            .trim()
            .map { c ->
                when (c) {
                    'à', 'â', 'ä' -> 'a'
                    'é', 'è', 'ê', 'ë' -> 'e'
                    'î', 'ï' -> 'i'
                    'ô', 'ö' -> 'o'
                    'ù', 'û', 'ü' -> 'u'
                    'ç' -> 'c'
                    else -> c
                }
            }
            .joinToString("")
}
