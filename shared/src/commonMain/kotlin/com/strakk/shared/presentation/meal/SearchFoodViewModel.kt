package com.strakk.shared.presentation.meal

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.usecase.SearchFoodUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val DEBOUNCE_MS = 250L

/**
 * Debounced search over the user's history + CIQUAL catalogue.
 *
 * The query text is fed through a [MutableStateFlow] with 250 ms debounce to
 * avoid spamming the DB on every keystroke.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchFoodViewModel(
    private val searchFood: SearchFoodUseCase,
) : MviViewModel<SearchFoodUiState, SearchFoodEvent, SearchFoodEffect>(SearchFoodUiState.Loading) {

    private val queryFlow = MutableStateFlow("")

    init {
        queryFlow
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                searchFood(query).catch { emit(emptyResult()) }
            }
            .onEach { results ->
                setState {
                    SearchFoodUiState.Ready(
                        query = queryFlow.value,
                        results = results,
                        isSearching = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: SearchFoodEvent) = when (event) {
        is SearchFoodEvent.QueryChanged -> handleQueryChanged(event.query)
        is SearchFoodEvent.SelectUserItem -> emit(
            SearchFoodEffect.ItemSelected(name = event.normalizedName, sourceId = event.normalizedName),
        )
        is SearchFoodEvent.SelectCatalogItem -> emit(
            SearchFoodEffect.ItemSelected(name = "catalog", sourceId = event.id.toString()),
        )
        SearchFoodEvent.Retry -> queryFlow.value = queryFlow.value
    }

    private fun handleQueryChanged(query: String) {
        queryFlow.value = query
        setState {
            when (this) {
                is SearchFoodUiState.Ready -> copy(query = query, isSearching = true)
                else -> SearchFoodUiState.Ready(
                    query = query,
                    results = emptyResult(),
                    isSearching = true,
                )
            }
        }
    }

    private fun emptyResult() = SearchFoodUseCase.SearchResults(
        userItems = emptyList(),
        catalogItems = emptyList(),
    )
}
