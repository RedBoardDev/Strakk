package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.ProfileRepository

/**
 * Retrieves the Hevy API key stored on the user's profile.
 *
 * @return [Result.success] with the API key string, or `null` if not yet configured.
 *   [Result.failure] on network or database errors.
 */
class GetHevyApiKeyUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(): Result<String?> =
        runSuspendCatching {
            profileRepository.getProfile()?.hevyApiKey
        }
}
