package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.repository.AuthRepository

/**
 * Validates credentials and creates a new user account with email and password.
 *
 * Returns [Result.success] on successful sign-up, or [Result.failure] with:
 * - [DomainError.ValidationError] if email format is invalid.
 * - Any exception thrown by the repository on network or auth errors.
 */
class SignUpUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val trimmedEmail = email.trim()

        if (!isValidEmail(trimmedEmail)) {
            return Result.failure(
                DomainError.ValidationError("Invalid email format"),
            )
        }

        return runSuspendCatching {
            authRepository.signUp(trimmedEmail, password)
        }
    }

    private fun isValidEmail(email: String): Boolean =
        email.contains("@") && email.substringAfter("@").contains(".")
}
