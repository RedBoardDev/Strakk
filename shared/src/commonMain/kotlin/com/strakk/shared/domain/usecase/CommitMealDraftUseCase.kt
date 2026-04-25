package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.repository.MealDraftRepository
import com.strakk.shared.domain.repository.MealRepository
import kotlinx.coroutines.flow.first

/**
 * Commits the processed draft to Supabase (Phase 2 of the commit flow).
 *
 * Precondition: all draft items must already be [DraftItem.Resolved]
 * (i.e. [ProcessMealDraftUseCase] has been called and the user has confirmed
 * the review).
 *
 * Steps:
 *   1. INSERT `meals` row.
 *   2. INSERT N `meal_entries` rows (with `meal_id`, `photo_path` where applicable).
 *   3. Purge the local draft (base64 + uploadedPaths).
 *
 * @return The committed [Meal].
 * @throws [DomainError.DataError] if the draft has unresolved (pending) items.
 */
class CommitMealDraftUseCase(
    private val draftRepository: MealDraftRepository,
    private val mealRepository: MealRepository,
) {
    suspend operator fun invoke(): Result<Meal> =
        runSuspendCatching {
            val draft = draftRepository.observeActiveDraft().first()
                ?: throw DomainError.DataError("No active draft to commit.")

            val pendingCount = draft.items.count {
                it is DraftItem.PendingPhoto || it is DraftItem.PendingText
            }
            if (pendingCount > 0) {
                throw DomainError.DataError(
                    "Draft still has $pendingCount pending item(s). Run ProcessMealDraftUseCase first.",
                )
            }

            val resolvedEntries = draft.items.filterIsInstance<DraftItem.Resolved>()

            val meal = mealRepository.commitMealDraft(
                draftId = draft.id,
                name = draft.name,
                date = draft.date,
                entries = resolvedEntries,
                photoPathsByItemId = draft.uploadedPaths,
            )

            draftRepository.discard()
            meal
        }
}
