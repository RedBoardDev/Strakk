package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.FavoriteMeal
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.first

/**
 * Toggles a [FavoriteMeal] template for the given [Meal] container.
 *
 * If a favorite already exists with `sourceMealId == meal.id`, it is removed.
 * Otherwise a new favorite is captured from the meal's items.
 *
 * Returns the resulting favorite (`null` when toggled off).
 */
class ToggleFavoriteMealUseCase(private val favoritesRepository: FavoritesRepository) {

    suspend operator fun invoke(meal: Meal): Result<FavoriteMeal?> = runSuspendCatching {
        if (meal.entries.isEmpty()) return@runSuspendCatching null
        val current = favoritesRepository.observeFavoriteMeals().first()
        val existing = current.firstOrNull { it.sourceMealId == meal.id }
        if (existing != null) {
            favoritesRepository.removeFavoriteMealById(existing.id)
            null
        } else {
            favoritesRepository.addFavoriteMeal(meal)
        }
    }
}
