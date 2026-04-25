package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.repository.MealDraftRepository
import kotlinx.coroutines.flow.Flow

/** Observes the current active meal draft, emitting null when none exists. */
class ObserveActiveMealDraftUseCase(
    private val draftRepository: MealDraftRepository,
) {
    operator fun invoke(): Flow<ActiveMealDraft?> = draftRepository.observeActiveDraft()
}
