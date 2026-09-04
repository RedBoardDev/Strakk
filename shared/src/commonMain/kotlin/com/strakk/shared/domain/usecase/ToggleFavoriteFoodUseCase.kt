package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.FavoriteFood
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.first

/**
 * Toggles a [FavoriteFood] for the given input.
 *
 * If a favorite already exists for the normalized name, it is removed.
 * Otherwise a new favorite is created from the supplied template.
 *
 * Returns the resulting favorite (`null` when toggled off).
 */
class ToggleFavoriteFoodUseCase(private val favoritesRepository: FavoritesRepository) {

    /** Toggle from a raw [MealEntry] (e.g. the Today screen). */
    suspend fun fromEntry(entry: MealEntry): Result<FavoriteFood?> = toggle(
        name = entry.name.orEmpty(),
        protein = entry.protein,
        calories = entry.calories,
        fat = entry.fat,
        carbs = entry.carbs,
        quantity = entry.quantity,
        foodCatalogId = entry.foodCatalogId,
    )

    /** Toggle from explicit fields (e.g. the search drawer or catalog row). */
    @Suppress("LongParameterList")
    suspend fun toggle(
        name: String,
        protein: Double,
        calories: Double,
        fat: Double?,
        carbs: Double?,
        quantity: String?,
        foodCatalogId: Long? = null,
    ): Result<FavoriteFood?> = runSuspendCatching {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@runSuspendCatching null
        val normalized = normalize(trimmed)
        val current = favoritesRepository.observeFavoriteFoods().first()
        val existing = current.firstOrNull { it.normalizedName == normalized }
        if (existing != null) {
            favoritesRepository.removeFavoriteFoodByName(normalized)
            null
        } else {
            favoritesRepository.addFavoriteFood(
                name = trimmed,
                protein = protein,
                calories = calories,
                fat = fat,
                carbs = carbs,
                quantity = quantity,
                foodCatalogId = foodCatalogId,
            )
        }
    }

    private fun normalize(text: String): String = normalizeFoodName(text)
}

private fun normalizeFoodName(text: String): String = text.lowercase().map { ch ->
    when (ch) {
        'à', 'â', 'ä' -> 'a'
        'é', 'è', 'ê', 'ë' -> 'e'
        'î', 'ï' -> 'i'
        'ô', 'ö' -> 'o'
        'ù', 'û', 'ü' -> 'u'
        'ç' -> 'c'
        else -> ch
    }
}.joinToString("")
