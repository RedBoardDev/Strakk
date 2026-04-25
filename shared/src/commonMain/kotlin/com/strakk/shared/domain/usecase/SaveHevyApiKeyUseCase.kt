package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.ProfileRepository

/**
 * Persists the Hevy API key to the user's profile.
 *
 * @return [Result.success] with [Unit] on success, or [Result.failure] on network/database errors.
 */
class SaveHevyApiKeyUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(apiKey: String): Result<Unit> =
        runSuspendCatching {
            profileRepository.updateHevyApiKey(apiKey)
        }
}
