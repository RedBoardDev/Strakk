package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.ManualEntryDraft
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.MealEntryInput
import com.strakk.shared.domain.model.NutritionDefaults
import com.strakk.shared.domain.repository.NutritionRepository

/**
 * Persists a user-filled manual entry as an orphan quick-add.
 *
 * All validation happens here so the UI only shows a generic error on failure —
 * detailed messages are attached to [DomainError.ValidationError] for the
 * ViewModel to display inline.
 */
class QuickAddManualUseCase(
    private val nutritionRepository: NutritionRepository,
    private val buildMealEntry: BuildMealEntryUseCase,
) {
    suspend operator fun invoke(draft: ManualEntryDraft): Result<MealEntry> =
        runSuspendCatching {
            validate(draft)
            val entry = buildMealEntry(
                MealEntryInput.Known(
                    name = draft.name,
                    protein = draft.protein,
                    calories = draft.calories,
                    fat = draft.fat,
                    carbs = draft.carbs,
                    quantity = draft.quantity,
                    source = EntrySource.Manual,
                    logDate = draft.logDate,
                ),
            )
            nutritionRepository.addMeal(entry)
        }

    private fun validate(draft: ManualEntryDraft) {
        if (draft.name.isBlank()) {
            throw DomainError.ValidationError("Name is required.")
        }
        if (draft.name.length > 100) {
            throw DomainError.ValidationError("Name must not exceed 100 characters.")
        }
        if (draft.protein < 0 || draft.protein > NutritionDefaults.MAX_MACRO_GRAMS) {
            throw DomainError.ValidationError("Protein must be between 0 and ${NutritionDefaults.MAX_MACRO_GRAMS}g.")
        }
        if (draft.calories < 0 || draft.calories > NutritionDefaults.MAX_CALORIES_ENTRY) {
            throw DomainError.ValidationError("Calories must be between 0 and ${NutritionDefaults.MAX_CALORIES_ENTRY} kcal.")
        }
        draft.fat?.let {
            if (it < 0 || it > NutritionDefaults.MAX_MACRO_GRAMS) {
                throw DomainError.ValidationError("Fat must be between 0 and ${NutritionDefaults.MAX_MACRO_GRAMS}g.")
            }
        }
        draft.carbs?.let {
            if (it < 0 || it > NutritionDefaults.MAX_MACRO_GRAMS) {
                throw DomainError.ValidationError("Carbs must be between 0 and ${NutritionDefaults.MAX_MACRO_GRAMS}g.")
            }
        }
    }
}
