package com.strakk.shared.presentation.meal

import com.strakk.shared.domain.usecase.SearchFoodUseCase.SearchResults

sealed interface SearchFoodUiState {
    data object Loading : SearchFoodUiState
    data class Ready(
        val query: String,
        val results: SearchResults,
        val isSearching: Boolean = false,
    ) : SearchFoodUiState
    data class Error(val message: String) : SearchFoodUiState
}

sealed interface SearchFoodEvent {
    data class QueryChanged(val query: String) : SearchFoodEvent
    data class SelectUserItem(val normalizedName: String) : SearchFoodEvent
    data class SelectCatalogItem(val id: Long) : SearchFoodEvent
    data object Retry : SearchFoodEvent
}

sealed interface SearchFoodEffect {
    data class ItemSelected(val name: String, val sourceId: String) : SearchFoodEffect
    data class ShowError(val message: String) : SearchFoodEffect
}
