package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.AuthRepository
import com.strakk.shared.domain.repository.MealDraftRepository
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.NutritionRepository
import com.strakk.shared.domain.repository.ProfileRepository

/**
 * Signs out the current user, clears the local session, and invalidates all
 * in-memory caches and the local draft so the next authenticated user starts
 * with a clean state.
 *
 * Returns [Result.success] on success, or [Result.failure] on network errors.
 */
class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val nutritionRepository: NutritionRepository,
    private val profileRepository: ProfileRepository,
    private val mealRepository: MealRepository,
    private val mealDraftRepository: MealDraftRepository,
) {
    suspend operator fun invoke(): Result<Unit> =
        runSuspendCatching {
            authRepository.signOut()
            nutritionRepository.clearCache()
            profileRepository.clearCache()
            mealRepository.clearCache()
            mealDraftRepository.discard()
        }
}
