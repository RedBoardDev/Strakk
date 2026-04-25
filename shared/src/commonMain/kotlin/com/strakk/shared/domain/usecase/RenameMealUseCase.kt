package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.MealRepository

private const val MIN_NAME_LENGTH = 1
private const val MAX_NAME_LENGTH = 60

/**
 * Renames a Processed meal.
 *
 * The server `CHECK` constraint enforces length 1..60 — this use case validates
 * client-side so the UI can show immediate feedback without a round-trip.
 */
class RenameMealUseCase(private val mealRepository: MealRepository) {
    suspend operator fun invoke(id: String, name: String): Result<Unit> = runSuspendCatching {
        val trimmed = name.trim()
        if (trimmed.length !in MIN_NAME_LENGTH..MAX_NAME_LENGTH) {
            throw DomainError.ValidationError(
                "Le nom du repas doit contenir entre $MIN_NAME_LENGTH et $MAX_NAME_LENGTH caractères.",
            )
        }
        mealRepository.renameMeal(id, trimmed)
    }
}
