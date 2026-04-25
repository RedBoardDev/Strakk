package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.MealDraftRepository

/** Discards the active draft and clears all persisted draft state. */
class DiscardMealDraftUseCase(
    private val draftRepository: MealDraftRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        runSuspendCatching { draftRepository.discard() }
}
