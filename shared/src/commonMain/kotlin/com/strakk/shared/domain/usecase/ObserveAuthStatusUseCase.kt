package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.AuthStatus
import com.strakk.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the authentication session status as a reactive stream.
 *
 * Delegates directly to [AuthRepository.observeSessionStatus].
 */
class ObserveAuthStatusUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthStatus> =
        authRepository.observeSessionStatus()
}
