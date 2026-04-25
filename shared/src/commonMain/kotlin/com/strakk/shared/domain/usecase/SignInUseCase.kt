package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.repository.AuthRepository

/**
 * Validates credentials and signs in an existing user with email and password.
 *
 * Returns [Result.success] on successful sign-in, or [Result.failure] with:
 * - [DomainError.ValidationError] if email format is invalid or password is blank.
 * - Any exception thrown by the repository on network or auth errors.
 */
class SignInUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val trimmedEmail = email.trim()

        if (!isValidEmail(trimmedEmail)) {
            return Result.failure(
                DomainError.ValidationError("Invalid email format"),
            )
        }
        if (password.isBlank()) {
            return Result.failure(
                DomainError.ValidationError("Password is required"),
            )
        }

        return runSuspendCatching {
            authRepository.signIn(trimmedEmail, password)
        }
    }

    private fun isValidEmail(email: String): Boolean =
        email.contains("@") && email.substringAfter("@").contains(".")
}
