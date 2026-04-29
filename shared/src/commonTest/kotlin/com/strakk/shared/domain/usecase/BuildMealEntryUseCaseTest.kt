package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.MealEntryInput
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BuildMealEntryUseCaseTest {

    private val clock = object : ClockProvider {
        override fun today(): LocalDate = LocalDate(2026, 4, 25)
        override fun now(): Instant = Instant.parse("2026-04-25T12:34:56Z")
    }

    @Test
    fun buildsEntryFromKnownInputUsingClockDateAndTrimmingTextFields() {
        val entry = BuildMealEntryUseCase(clock)(
            MealEntryInput.Known(
                name = "  Banane  ",
                protein = 1.2,
                calories = 90.0,
                fat = null,
                carbs = 20.0,
                quantity = "  100g  ",
                source = EntrySource.Search,
            ),
        )

        assertEquals("", entry.id)
        assertEquals("2026-04-25", entry.logDate)
        assertEquals("Banane", entry.name)
        assertEquals(1.2, entry.protein)
        assertEquals(90.0, entry.calories)
        assertEquals(20.0, entry.carbs)
        assertEquals("100g", entry.quantity)
        assertEquals(EntrySource.Search, entry.source)
        assertEquals("2026-04-25T12:34:56Z", entry.createdAt)
        assertEquals(null, entry.mealId)
    }

    @Test
    fun buildsEntryForExplicitDateAndMealId() {
        val entry = BuildMealEntryUseCase(clock)(
            MealEntryInput.Known(
                name = "Yaourt",
                protein = 8.0,
                calories = 120.0,
                fat = 2.0,
                carbs = 12.0,
                quantity = null,
                source = EntrySource.Barcode,
                logDate = "2026-04-20",
                mealId = "meal-1",
            ),
            localId = "local-entry",
        )

        assertEquals("local-entry", entry.id)
        assertEquals("2026-04-20", entry.logDate)
        assertEquals("meal-1", entry.mealId)
    }
}
