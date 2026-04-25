package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.MealRepository

/**
 * Adds a single entry to an already-committed Meal.
 *
 * Enforces the spec rule (D — cycle de vie B) that post-commit additions must
 * come from 0-call sources only (Search, Barcode, Manual, Frequent). Photo or
 * text AI additions are rejected here — the user must create a new Draft
 * instead.
 */
class AddItemToProcessedMealUseCase(
    private val mealRepository: MealRepository,
) {
    suspend operator fun invoke(mealId: String, entry: MealEntry): Result<Unit> =
        runSuspendCatching {
            if (entry.source == EntrySource.PhotoAi || entry.source == EntrySource.TextAi) {
                throw DomainError.ValidationError(
                    "Impossible d'ajouter un item IA à un repas terminé. " +
                        "Créez un nouveau repas pour ça.",
                )
            }
            mealRepository.addEntryToMeal(
                mealId = mealId,
                entry = DraftItem.Resolved(id = entry.id.ifBlank { generateEntryId() }, entry = entry),
            )
        }

    private fun generateEntryId(): String =
        kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toString()
}
