package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching

import com.strakk.shared.domain.model.OnboardingData
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.repository.ProfileRepository

/**
 * Creates a new user profile with onboarding data.
 *
 * Called at the end of the onboarding flow to persist goals
 * and reminder preferences.
 *
 * Returns [Result.success] with the created [UserProfile],
 * or [Result.failure] on network or database errors.
 */
class CreateProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(data: OnboardingData): Result<UserProfile> =
        runSuspendCatching {
            profileRepository.createProfile(data)
        }
}
