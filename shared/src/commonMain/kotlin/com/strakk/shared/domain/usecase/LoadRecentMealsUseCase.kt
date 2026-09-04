package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.RecentMeal
import com.strakk.shared.domain.repository.FavoritesRepository

/**
 * Loads the user's most recent distinct meals for the Search drawer's
 * "Recent meals" section. Result is one-shot (no observable cache).
 */
class LoadRecentMealsUseCase(private val favoritesRepository: FavoritesRepository) {
    suspend operator fun invoke(daysWindow: Int = DEFAULT_DAYS, limit: Int = DEFAULT_LIMIT): Result<List<RecentMeal>> {
        return runSuspendCatching { favoritesRepository.loadRecentMeals(daysWindow, limit) }
    }

    private companion object {
        const val DEFAULT_DAYS = 30
        const val DEFAULT_LIMIT = 20
    }
}
