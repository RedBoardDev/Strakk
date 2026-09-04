package com.strakk.shared.presentation.meal

import com.strakk.shared.domain.model.FavoriteFood
import com.strakk.shared.domain.model.FavoriteMeal
import com.strakk.shared.domain.model.FoodCatalogItem
import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.model.MealTemplateItem
import com.strakk.shared.domain.model.RecentMeal

/** Top-level tab in the search drawer. */
enum class SearchTab { MyFoods, Catalog }

/** Stable identifier of a meal template, surfaced from "My foods". */
sealed interface MealTemplateRef {
    val name: String
    val items: List<MealTemplateItem>

    /** A meal already favorited by the user. */
    data class Favorite(val id: String, override val name: String, override val items: List<MealTemplateItem>) :
        MealTemplateRef

    /** A previously-logged meal returned by the recent_meals_v1 RPC. */
    data class Recent(val sourceMealId: String, override val name: String, override val items: List<MealTemplateItem>) :
        MealTemplateRef
}

/** Aggregated data for the "My foods" tab. */
data class MyFoodsData(
    val favoriteMeals: List<FavoriteMeal>,
    val favoriteFoods: List<FavoriteFood>,
    val recentMeals: List<RecentMeal>,
    val recentFoods: List<FrequentItem>,
)

/** Aggregated data for the "Catalog" tab. */
data class CatalogData(
    val items: List<FoodCatalogItem>,
)

sealed interface SearchFoodUiState {
    data object Loading : SearchFoodUiState

    data class Ready(
        val selectedTab: SearchTab,
        val query: String,
        val myFoods: MyFoodsData,
        val catalog: CatalogData,
        /** True while a catalog search is in-flight (My foods is filtered locally). */
        val isSearching: Boolean = false,
    ) : SearchFoodUiState

    data class Error(val message: String) : SearchFoodUiState
}

sealed interface SearchFoodEvent {
    data class SwitchTab(val tab: SearchTab) : SearchFoodEvent
    data class QueryChanged(val query: String) : SearchFoodEvent

    /** Toggle the favorite state of a food item from any row. */
    data class ToggleFavoriteFood(
        val name: String,
        val protein: Double,
        val calories: Double,
        val fat: Double?,
        val carbs: Double?,
        val quantity: String?,
        val foodCatalogId: Long? = null,
    ) : SearchFoodEvent

    /** Toggle the favorite state of an existing favorite-meal by its id. */
    data class UnfavoriteMeal(val favoriteId: String) : SearchFoodEvent

    /** Add a meal template (favorite or recent) as a new committed meal for today. */
    data class AddMealTemplate(val ref: MealTemplateRef, val logDate: String?) : SearchFoodEvent

    data object Retry : SearchFoodEvent
}

sealed interface SearchFoodEffect {
    data class FoodSelected(val name: String) : SearchFoodEffect
    data object MealTemplateAdded : SearchFoodEffect
    data class ShowError(val message: String) : SearchFoodEffect
}
