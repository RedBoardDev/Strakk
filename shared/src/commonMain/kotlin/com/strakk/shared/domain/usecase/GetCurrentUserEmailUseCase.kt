package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching

import com.strakk.shared.domain.repository.AuthRepository

/**
 * Retrieves the email address of the currently authenticated user.
 *
 * @return [Result.success] with the email (or `null` if no session), or [Result.failure] on error.
 */
class GetCurrentUserEmailUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<String?> = runSuspendCatching {
        authRepository.getCurrentUserEmail()
    }
}
