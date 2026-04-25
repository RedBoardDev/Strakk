package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.FoodCatalogItem

/**
 * Repository for reading from the `food_catalog` table (CIQUAL data).
 *
 * No cache — queries are direct PostgREST calls.
 */
interface FoodCatalogRepository {

    /**
     * Searches the catalogue by [query] (substring match on `name_normalized`).
     *
     * @param query Search string; normalized server-side.
     * @param limit Maximum number of results (default 20).
     * @return Matching [FoodCatalogItem] list, ordered by relevance.
     */
    suspend fun search(query: String, limit: Int = 20): List<FoodCatalogItem>
}
