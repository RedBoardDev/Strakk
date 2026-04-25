package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.repository.FoodCatalogRepository
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Unified search across the user's logging history and the shared food catalogue.
 *
 * Empty/blank query → returns the user's top [MAX_FREQUENTS] most-frequent items
 * and an empty catalogue list (catalogue search requires a query).
 *
 * Non-empty query → filters user frequents locally (substring match on
 * normalized name) and queries the CIQUAL catalogue via Supabase.
 */
class SearchFoodUseCase(
    private val nutritionRepository: NutritionRepository,
    private val foodCatalogRepository: FoodCatalogRepository,
) {
    data class SearchResults(
        val userItems: List<FrequentItem>,
        val catalogItems: List<FoodCatalogItem>,
    )

    operator fun invoke(query: String, limit: Int = DEFAULT_LIMIT): Flow<SearchResults> {
        val trimmed = query.trim()

        if (trimmed.isEmpty()) {
            return nutritionRepository.observeFrequentItems(MAX_FREQUENTS).map { frequents ->
                SearchResults(userItems = frequents, catalogItems = emptyList())
            }
        }

        val normalized = normalize(trimmed)
        val userItemsFlow = nutritionRepository.observeFrequentItems(MAX_FREQUENTS).map { items ->
            items.filter { it.normalizedName.contains(normalized) }.take(limit)
        }
        val catalogFlow = flow {
            emit(foodCatalogRepository.search(trimmed, limit))
        }

        return combine(userItemsFlow, catalogFlow) { user, catalog ->
            SearchResults(userItems = user, catalogItems = catalog)
        }
    }

    private fun normalize(text: String): String =
        text.lowercase().map { c ->
            when (c) {
                'à', 'â', 'ä' -> 'a'
                'é', 'è', 'ê', 'ë' -> 'e'
                'î', 'ï' -> 'i'
                'ô', 'ö' -> 'o'
                'ù', 'û', 'ü' -> 'u'
                'ç' -> 'c'
                else -> c
            }
        }.joinToString("")

    companion object {
        private const val DEFAULT_LIMIT = 20
        private const val MAX_FREQUENTS = 30
    }
}
