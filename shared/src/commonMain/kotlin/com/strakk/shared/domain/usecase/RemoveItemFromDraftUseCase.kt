package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.MealDraftRepository

/** Removes an item from the active draft by its ID. */
class RemoveItemFromDraftUseCase(
    private val draftRepository: MealDraftRepository,
) {
    suspend operator fun invoke(itemId: String): Result<Unit> =
        runSuspendCatching { draftRepository.removeItem(itemId) }
}
