package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.repository.ProfileRepository
import com.strakk.shared.domain.common.runSuspendCatching

/**
 * Updates the current user's profile with the given goals.
 *
 * Called from the Settings screen. Each field change is debounced by the ViewModel
 * before invoking this use case. Null values are stored as NULL (clearing the field).
 *
 * Returns [Result.success] with the updated [UserProfile],
 * or [Result.failure] on network or database errors.
 */
class UpdateProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(
        proteinGoal: Int?,
        calorieGoal: Int?,
        waterGoal: Int?,
    ): Result<UserProfile> = runSuspendCatching {
        profileRepository.updateProfile(
            proteinGoal = proteinGoal,
            calorieGoal = calorieGoal,
            waterGoal = waterGoal,
        )
    }
}
