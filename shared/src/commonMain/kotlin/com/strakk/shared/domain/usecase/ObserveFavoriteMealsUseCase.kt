package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.FavoriteMeal
import com.strakk.shared.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteMealsUseCase(private val favoritesRepository: FavoritesRepository) {
    operator fun invoke(): Flow<List<FavoriteMeal>> = favoritesRepository.observeFavoriteMeals()
}
