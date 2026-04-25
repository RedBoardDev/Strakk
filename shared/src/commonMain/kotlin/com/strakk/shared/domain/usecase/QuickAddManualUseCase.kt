package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.ManualEntryDraft
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Persists a user-filled manual entry as an orphan quick-add.
 *
 * All validation happens here so the UI only shows a generic error on failure —
 * detailed messages are attached to [DomainError.ValidationError] for the
 * ViewModel to display inline.
 */
class QuickAddManualUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(draft: ManualEntryDraft): Result<MealEntry> =
        runSuspendCatching {
            validate(draft)
            val today = todayIso()
            val entry = MealEntry(
                id = "",
                logDate = today,
                name = draft.name.trim(),
                protein = draft.protein,
                calories = draft.calories,
                fat = draft.fat,
                carbs = draft.carbs,
                source = EntrySource.Manual,
                createdAt = "",
                mealId = null,
                quantity = draft.quantity?.trim()?.ifBlank { null },
                breakdown = null,
                photoPath = null,
            )
            nutritionRepository.addMeal(entry)
        }

    private fun validate(draft: ManualEntryDraft) {
        if (draft.name.isBlank()) {
            throw DomainError.ValidationError("Un nom est requis.")
        }
        if (draft.name.length > 100) {
            throw DomainError.ValidationError("Le nom ne peut pas dépasser 100 caractères.")
        }
        if (draft.protein < 0 || draft.protein > 500) {
            throw DomainError.ValidationError("Les protéines doivent être entre 0 et 500g.")
        }
        if (draft.calories < 0 || draft.calories > 5000) {
            throw DomainError.ValidationError("Les calories doivent être entre 0 et 5000 kcal.")
        }
        draft.fat?.let {
            if (it < 0 || it > 500) {
                throw DomainError.ValidationError("Les lipides doivent être entre 0 et 500g.")
            }
        }
        draft.carbs?.let {
            if (it < 0 || it > 500) {
                throw DomainError.ValidationError("Les glucides doivent être entre 0 et 500g.")
            }
        }
    }

    private fun todayIso(): String =
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
}
