package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the current user's profile.
 *
 * The underlying repository fetches from the network on first subscription and
 * updates the Flow whenever the profile is mutated (create/update).
 *
 * @return A [Flow] emitting the current [UserProfile], or null if none exists.
 */
class ObserveProfileUseCase(private val profileRepository: ProfileRepository) {
    operator fun invoke(): Flow<UserProfile?> = profileRepository.observeProfile()
}
