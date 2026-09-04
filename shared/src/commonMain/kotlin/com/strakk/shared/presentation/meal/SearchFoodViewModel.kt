package com.strakk.shared.presentation.meal

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.model.FavoriteFood
import com.strakk.shared.domain.model.FavoriteMeal
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.model.MealTemplateItem
import com.strakk.shared.domain.model.RecentMeal
import com.strakk.shared.domain.repository.FavoritesRepository
import com.strakk.shared.domain.repository.FoodCatalogRepository
import com.strakk.shared.domain.usecase.AddMealFromTemplateUseCase
import com.strakk.shared.domain.usecase.LoadRecentFoodsUseCase
import com.strakk.shared.domain.usecase.LoadRecentMealsUseCase
import com.strakk.shared.domain.usecase.ObserveFavoriteFoodsUseCase
import com.strakk.shared.domain.usecase.ObserveFavoriteMealsUseCase
import com.strakk.shared.domain.usecase.ObserveFrequentItemsUseCase
import com.strakk.shared.domain.usecase.ToggleFavoriteFoodUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val DEBOUNCE_MS = 250L
private const val CATALOG_LIMIT = 20
private const val MY_FOODS_VISIBLE_LIMIT = 30
private const val MIN_CATALOG_QUERY = 1

/**
 * Drives the new tabbed search drawer.
 *
 * - **My foods** (default) — aggregates user favorites + recent meals + recent
 *   foods. Search is local, instant, no network on keystroke.
 * - **Catalog** — debounced CIQUAL/OFF search.
 *
 * Favorites are reactive: hearting an item anywhere (Today, search drawer,
 * MealDetailSheet) flows back through the `FavoritesRepository` cache and the
 * UI updates without an explicit reload.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchFoodViewModel(
    private val observeFavoriteFoods: ObserveFavoriteFoodsUseCase,
    private val observeFavoriteMeals: ObserveFavoriteMealsUseCase,
    private val observeFrequentItems: ObserveFrequentItemsUseCase,
    private val loadRecentMealsUseCase: LoadRecentMealsUseCase,
    private val loadRecentFoodsUseCase: LoadRecentFoodsUseCase,
    private val toggleFavoriteFood: ToggleFavoriteFoodUseCase,
    private val favoritesRepository: FavoritesRepository,
    private val addMealFromTemplate: AddMealFromTemplateUseCase,
    private val foodCatalogRepository: FoodCatalogRepository,
    private val clock: ClockProvider,
) : MviViewModel<SearchFoodUiState, SearchFoodEvent, SearchFoodEffect>(SearchFoodUiState.Loading) {

    private val tab = MutableStateFlow(SearchTab.MyFoods)
    private val query = MutableStateFlow("")
    private val recentMeals = MutableStateFlow<List<RecentMeal>>(emptyList())
    private val recentFoodsExtra = MutableStateFlow<List<MealTemplateItem>>(emptyList())
    private val catalog = MutableStateFlow<List<FoodCatalogItem>>(emptyList())
    private val isSearching = MutableStateFlow(false)

    init {
        wireState()
        wireCatalogSearch()
        viewModelScope.launch { refreshRecents() }
    }

    override fun onEvent(event: SearchFoodEvent) {
        when (event) {
            is SearchFoodEvent.SwitchTab -> tab.value = event.tab
            is SearchFoodEvent.QueryChanged -> query.value = event.query
            is SearchFoodEvent.ToggleFavoriteFood -> handleToggleFavoriteFood(event)
            is SearchFoodEvent.UnfavoriteMeal -> handleUnfavoriteMeal(event.favoriteId)
            is SearchFoodEvent.AddMealTemplate -> handleAddMealTemplate(event)
            SearchFoodEvent.Retry -> {
                query.value = query.value
                viewModelScope.launch { refreshRecents() }
            }
        }
    }

    private fun wireState() {
        val myFoodsFlow = combine(
            observeFavoriteFoods(),
            observeFavoriteMeals(),
            observeFrequentItems(MY_FOODS_VISIBLE_LIMIT),
            recentMeals,
            recentFoodsExtra,
        ) { favFoods, favMeals, frequents, recents, extraFoods ->
            arrayOf(favFoods, favMeals, frequents, recents, extraFoods)
        }

        combine(
            tab,
            query,
            myFoodsFlow,
            catalog,
            isSearching,
        ) { selectedTab, q, myFoodsData, catalogItems, searching ->
            @Suppress("UNCHECKED_CAST", "MagicNumber")
            val favFoods = myFoodsData[0] as List<FavoriteFood>

            @Suppress("UNCHECKED_CAST", "MagicNumber")
            val favMeals = myFoodsData[1] as List<FavoriteMeal>

            @Suppress("UNCHECKED_CAST", "MagicNumber")
            val frequents = myFoodsData[2] as List<FrequentItem>

            @Suppress("UNCHECKED_CAST", "MagicNumber")
            val recents = myFoodsData[3] as List<RecentMeal>

            @Suppress("UNCHECKED_CAST", "MagicNumber")
            val extras = myFoodsData[4] as List<MealTemplateItem>

            SearchFoodUiState.Ready(
                selectedTab = selectedTab,
                query = q,
                myFoods = buildMyFoodsData(q, favFoods, favMeals, frequents, recents, extras),
                catalog = CatalogData(items = catalogItems),
                isSearching = searching,
            )
        }
            .distinctUntilChanged()
            .onEach { state -> setState { state } }
            .launchIn(viewModelScope)
    }

    private fun wireCatalogSearch() {
        query
            .debounce(DEBOUNCE_MS)
            .distinctUntilChanged()
            .flatMapLatest { q ->
                val trimmed = q.trim()
                if (trimmed.length < MIN_CATALOG_QUERY) {
                    catalog.value = emptyList()
                    isSearching.value = false
                    kotlinx.coroutines.flow.flowOf(emptyList<FoodCatalogItem>())
                } else {
                    isSearching.value = true
                    kotlinx.coroutines.flow.flow {
                        val results = runCatching { foodCatalogRepository.search(trimmed, CATALOG_LIMIT) }
                            .getOrElse { emptyList() }
                        emit(results)
                    }
                }
            }
            .onEach { results ->
                catalog.value = results
                isSearching.value = false
            }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshRecents() {
        loadRecentMealsUseCase().onSuccess { recentMeals.value = it }
        loadRecentFoodsUseCase().onSuccess { recentFoodsExtra.value = it }
    }

    @Suppress("LongParameterList", "LongMethod")
    private fun buildMyFoodsData(
        query: String,
        favFoods: List<FavoriteFood>,
        favMeals: List<FavoriteMeal>,
        frequents: List<FrequentItem>,
        recents: List<RecentMeal>,
        extraFoods: List<MealTemplateItem>,
    ): MyFoodsData {
        val normalized = normalize(query.trim())
        val filteredFavFoods = if (normalized.isEmpty()) {
            favFoods
        } else {
            favFoods.filter { it.normalizedName.contains(normalized) }
        }
        val filteredFavMeals = if (normalized.isEmpty()) {
            favMeals
        } else {
            favMeals.filter { normalize(it.name).contains(normalized) }
        }
        val filteredFrequents = if (normalized.isEmpty()) {
            frequents
        } else {
            frequents.filter { it.normalizedName.contains(normalized) }
        }
        val filteredRecentMeals = if (normalized.isEmpty()) {
            recents
        } else {
            recents.filter { normalize(it.name).contains(normalized) }
        }

        val frequentNames = frequents.map { it.normalizedName }.toSet()
        val extras = extraFoods
            .filter { item -> normalize(item.name) !in frequentNames }
            .map { item ->
                FrequentItem(
                    normalizedName = normalize(item.name),
                    name = item.name,
                    protein = item.protein,
                    calories = item.calories,
                    fat = item.fat,
                    carbs = item.carbs,
                    quantity = item.quantity,
                    occurrences = 1,
                )
            }
            .filter { if (normalized.isEmpty()) true else it.normalizedName.contains(normalized) }

        return MyFoodsData(
            favoriteMeals = filteredFavMeals,
            favoriteFoods = filteredFavFoods,
            recentMeals = filteredRecentMeals,
            recentFoods = filteredFrequents + extras,
        )
    }

    private fun handleToggleFavoriteFood(event: SearchFoodEvent.ToggleFavoriteFood) {
        viewModelScope.launch {
            toggleFavoriteFood
                .toggle(
                    name = event.name,
                    protein = event.protein,
                    calories = event.calories,
                    fat = event.fat,
                    carbs = event.carbs,
                    quantity = event.quantity,
                    foodCatalogId = event.foodCatalogId,
                )
                .onFailure { emit(SearchFoodEffect.ShowError(it.message ?: "Failed to update favorite")) }
        }
    }

    private fun handleUnfavoriteMeal(favoriteId: String) {
        viewModelScope.launch {
            runCatching { favoritesRepository.removeFavoriteMealById(favoriteId) }
                .onFailure { emit(SearchFoodEffect.ShowError(it.message ?: "Failed to remove favorite")) }
        }
    }

    private fun handleAddMealTemplate(event: SearchFoodEvent.AddMealTemplate) {
        viewModelScope.launch {
            addMealFromTemplate(
                name = event.ref.name,
                items = event.ref.items,
                logDate = event.logDate ?: clock.today().toString(),
            )
                .onSuccess {
                    emit(SearchFoodEffect.MealTemplateAdded)
                    refreshRecents()
                }
                .onFailure { emit(SearchFoodEffect.ShowError(it.message ?: "Failed to add meal")) }
        }
    }

    private fun normalize(text: String): String = normalizeSearchText(text)
}

private fun normalizeSearchText(text: String): String = text.lowercase().map { ch ->
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
