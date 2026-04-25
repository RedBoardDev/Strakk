package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.MealDraftRepository

/**
 * Replaces a draft item with updated nutritional values.
 *
 * The item must already be [DraftItem.Resolved]; pending items must be
 * processed first via [ProcessMealDraftUseCase].
 */
class UpdateDraftItemUseCase(
    private val draftRepository: MealDraftRepository,
) {
    suspend operator fun invoke(itemId: String, entry: MealEntry): Result<Unit> =
        runSuspendCatching { draftRepository.markItemResolved(itemId, entry) }
}
