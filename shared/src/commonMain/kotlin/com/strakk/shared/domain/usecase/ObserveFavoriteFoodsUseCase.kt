package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.FavoriteFood
import com.strakk.shared.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteFoodsUseCase(private val favoritesRepository: FavoritesRepository) {
    operator fun invoke(): Flow<List<FavoriteFood>> = favoritesRepository.observeFavoriteFoods()
}
