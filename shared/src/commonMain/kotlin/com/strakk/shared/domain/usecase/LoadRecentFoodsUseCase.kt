package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.MealTemplateItem
import com.strakk.shared.domain.repository.FavoritesRepository

/**
 * Loads the user's most recent distinct food items (deduped by normalized
 * name), sorted by recency descending. Used by the Search drawer's
 * "Recent foods" section.
 */
class LoadRecentFoodsUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(
        daysWindow: Int = DEFAULT_DAYS,
        limit: Int = DEFAULT_LIMIT,
    ): Result<List<MealTemplateItem>> {
        return runSuspendCatching { favoritesRepository.loadRecentFoods(daysWindow, limit) }
    }

    private companion object {
        const val DEFAULT_DAYS = 60
        const val DEFAULT_LIMIT = 30
    }
}
