package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntryInput
import com.strakk.shared.domain.model.MealTemplateItem
import com.strakk.shared.domain.repository.MealRepository

class AddMealFromTemplateUseCase(
    private val mealRepository: MealRepository,
    private val buildMealEntry: BuildMealEntryUseCase,
) {
    suspend operator fun invoke(name: String, items: List<MealTemplateItem>, logDate: String): Result<Meal> {
        return runSuspendCatching {
            require(items.isNotEmpty()) { "Cannot add a meal template with no items" }
            val resolved = items.mapIndexed { index, item -> resolveDraftItem(index, item, logDate) }
            mealRepository.commitMealDraft(
                draftId = "template-${kotlin.random.Random.nextLong()}",
                name = name,
                date = logDate,
                entries = resolved,
                photoPathsByItemId = emptyMap(),
            )
        }
    }

    private fun resolveDraftItem(index: Int, item: MealTemplateItem, logDate: String): DraftItem.Resolved {
        val itemId = "tpl-$index"
        return DraftItem.Resolved(
            id = itemId,
            entry = buildMealEntry(
                MealEntryInput.Known(
                    name = item.name,
                    protein = item.protein,
                    calories = item.calories,
                    fat = item.fat,
                    carbs = item.carbs,
                    quantity = item.quantity,
                    source = EntrySource.Frequent,
                    logDate = logDate,
                ),
                localId = itemId,
            ),
        )
    }
}
