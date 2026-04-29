package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.FrequentItem
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry
import kotlinx.coroutines.flow.Flow

/**
 * Operations on `meal_entries` and `water_entries`.
 *
 * Orphan entries (`meal_id = NULL`) represent quick-adds; entries attached to
 * a Meal container are managed through [MealRepository] instead — this
 * interface only exposes the timeline-wide views and water operations.
 */
interface NutritionRepository {

    // -------------------------------------------------------------------------
    // Reactive observation
    // -------------------------------------------------------------------------

    /**
     * Stream of orphan meal entries for [date], ordered by creation time.
     * Fetched lazily on first subscription per date; mutations emit
     * automatically.
     */
    fun observeMealsForDate(date: String): Flow<List<MealEntry>>

    /**
     * Stream of water entries for [date], ordered by creation time.
     */
    fun observeWaterEntriesForDate(date: String): Flow<List<WaterEntry>>

    /**
     * Stream emitting [Unit] on every successful mutation (meals or water).
     * Consumers use this to trigger dependent refreshes (e.g. calendar).
     */
    fun observeNutritionMutations(): Flow<Unit>

    /**
     * Stream of the top [limit] most-frequent food items the user has logged,
     * deduplicated by normalized name. Items are ranked by recency × frequency.
     *
     * Nutritional values reflect the most recent occurrence.
     */
    fun observeFrequentItems(limit: Int = 20): Flow<List<FrequentItem>>

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Inserts a meal entry for the current authenticated user.
     *
     * @return The created [MealEntry].
     */
    suspend fun addMeal(entry: MealEntry): MealEntry

    /** Deletes the meal entry with [id]. */
    suspend fun deleteMeal(id: String)

    /** Updates the editable fields of a meal entry in-place. */
    suspend fun updateMealEntry(entry: MealEntry): MealEntry

    /** Inserts a water entry. */
    suspend fun addWater(logDate: String, amount: Int): WaterEntry

    /** Deletes the water entry with [id]. */
    suspend fun deleteWater(id: String)

    // -------------------------------------------------------------------------
    // Calendar
    // -------------------------------------------------------------------------

    /**
     * Returns the distinct dates (ISO-8601) within [monthStart]..[monthEnd]
     * that have at least one meal or water entry.
     */
    suspend fun getActiveCalendarDays(monthStart: String, monthEnd: String): List<String>

    // -------------------------------------------------------------------------
    // Cache
    // -------------------------------------------------------------------------

    /** Clears all in-memory caches. Called on sign-out. */
    fun clearCache()
}
