package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching

import com.strakk.shared.domain.repository.ProfileRepository

/**
 * Checks whether the current user has a `profiles` row.
 *
 * Used after authentication to decide between onboarding (new user)
 * and home (returning user).
 *
 * Returns [Result.success] with `true` if the profile exists, `false` otherwise.
 * Returns [Result.failure] on network or database errors.
 */
class CheckProfileExistsUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): Result<Boolean> =
        runSuspendCatching {
            profileRepository.profileExists()
        }
}
