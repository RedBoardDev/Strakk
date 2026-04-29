package com.strakk.shared.data.repository

import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.MealEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class NutritionRepositoryImplTest {

    @Test
    fun mergeFetchedMealsKeepsOptimisticEntriesNotReturnedByFetch() {
        val optimistic = mealEntry(id = "local-created", createdAt = "2026-04-25T12:01:00Z")
        val fetched = mealEntry(id = "server-existing", createdAt = "2026-04-25T12:00:00Z")

        val merged = mergeFetchedMealEntries(
            cachedEntries = listOf(optimistic),
            fetchedEntries = listOf(fetched),
        )

        assertEquals(listOf(fetched, optimistic), merged)
    }

    @Test
    fun mergeFetchedMealsPrefersFetchedVersionWhenIdsMatch() {
        val stale = mealEntry(id = "same", name = "stale", calories = 10.0)
        val fresh = mealEntry(id = "same", name = "fresh", calories = 20.0)

        val merged = mergeFetchedMealEntries(
            cachedEntries = listOf(stale),
            fetchedEntries = listOf(fresh),
        )

        assertEquals(listOf(fresh), merged)
    }

    private fun mealEntry(
        id: String,
        name: String = id,
        calories: Double = 100.0,
        createdAt: String = "2026-04-25T12:00:00Z",
    ) = MealEntry(
        id = id,
        logDate = "2026-04-25",
        name = name,
        protein = 10.0,
        calories = calories,
        fat = null,
        carbs = null,
        source = EntrySource.Manual,
        createdAt = createdAt,
    )
}
