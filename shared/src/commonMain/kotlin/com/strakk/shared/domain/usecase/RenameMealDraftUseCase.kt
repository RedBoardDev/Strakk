package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.MealDraftRepository

/** Updates the display name of the active draft. */
class RenameMealDraftUseCase(
    private val draftRepository: MealDraftRepository,
) {
    suspend operator fun invoke(name: String): Result<Unit> =
        runSuspendCatching { draftRepository.rename(name) }
}
