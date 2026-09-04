package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.FavoriteFood
import com.strakk.shared.domain.model.FavoriteMeal
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealTemplateItem
import com.strakk.shared.domain.model.RecentMeal
import kotlinx.coroutines.flow.Flow

/**
 * Persists per-user favorite foods and meals + queries recent items for the
 * search drawer.
 *
 * Implementations cache reactively (same pattern as [NutritionRepository]):
 * [observeFavoriteFoods] / [observeFavoriteMeals] emit immediately from cache
 * and mutations propagate.
 */
@Suppress("TooManyFunctions", "LongParameterList")
interface FavoritesRepository {

    // ---------- Favorite foods ----------

    fun observeFavoriteFoods(): Flow<List<FavoriteFood>>

    /**
     * Adds (or returns existing) favorite food for the given input. The DB
     * enforces uniqueness on `(user_id, name_normalized)` so this is idempotent.
     */
    suspend fun addFavoriteFood(
        name: String,
        protein: Double,
        calories: Double,
        fat: Double?,
        carbs: Double?,
        quantity: String?,
        foodCatalogId: Long? = null,
    ): FavoriteFood

    /** Removes the favorite food whose [normalizedName] matches. No-op if absent. */
    suspend fun removeFavoriteFoodByName(normalizedName: String)

    // ---------- Favorite meals ----------

    fun observeFavoriteMeals(): Flow<List<FavoriteMeal>>

    /**
     * Captures the given meal as a favorite template. Multiple favorites with
     * the same name are allowed; uniqueness is keyed on row id only.
     */
    suspend fun addFavoriteMeal(meal: Meal): FavoriteMeal

    /** Removes the favorite whose [sourceMealId] matches the given id. */
    suspend fun removeFavoriteMealBySourceId(sourceMealId: String)

    /** Removes a favorite meal by its own id. */
    suspend fun removeFavoriteMealById(id: String)

    // ---------- Recents ----------

    /**
     * Returns the user's most recent distinct meals (latest occurrence per
     * meal name) within [daysWindow]. Includes items in chronological order.
     */
    suspend fun loadRecentMeals(daysWindow: Int = 30, limit: Int = 20): List<RecentMeal>

    /**
     * Returns the user's most recent distinct foods (latest occurrence per
     * normalized name) within [daysWindow], ordered by recency descending.
     */
    suspend fun loadRecentFoods(daysWindow: Int = 60, limit: Int = 30): List<MealTemplateItem>

    /** Clears all in-memory caches. Called on sign-out. */
    fun clearCache()
}
