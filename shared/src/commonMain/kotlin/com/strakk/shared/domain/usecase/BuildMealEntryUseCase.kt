package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.MealEntryInput

/**
 * Builds domain [MealEntry] instances from UI-safe creation intents.
 */
class BuildMealEntryUseCase(
    private val clock: ClockProvider,
) {
    operator fun invoke(input: MealEntryInput, localId: String = ""): MealEntry =
        when (input) {
            is MealEntryInput.Known -> MealEntry(
                id = localId,
                logDate = input.logDate ?: clock.today().toString(),
                name = input.name.trim().takeIf { it.isNotBlank() },
                protein = input.protein,
                calories = input.calories,
                fat = input.fat,
                carbs = input.carbs,
                source = input.source,
                createdAt = clock.now().toString(),
                mealId = input.mealId,
                quantity = input.quantity?.trim()?.takeIf { it.isNotBlank() },
            )
        }
}
