package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.repository.MealDraftRepository

/** Appends a [DraftItem] to the active draft. */
class AddItemToDraftUseCase(
    private val draftRepository: MealDraftRepository,
) {
    suspend operator fun invoke(item: DraftItem): Result<Unit> =
        runSuspendCatching { draftRepository.addItem(item) }
}
