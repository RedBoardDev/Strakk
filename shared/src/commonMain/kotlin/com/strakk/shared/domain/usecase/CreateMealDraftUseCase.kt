package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.repository.MealDraftRepository

/**
 * Creates a new empty meal draft and makes it the active one.
 *
 * Discards any existing draft first (per D1: one draft at a time).
 */
class CreateMealDraftUseCase(
    private val draftRepository: MealDraftRepository,
) {
    suspend operator fun invoke(name: String, date: String): Result<ActiveMealDraft> =
        runSuspendCatching {
            draftRepository.discard()
            draftRepository.createDraft(name = name, date = date)
        }
}
