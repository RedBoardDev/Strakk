package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.repository.AuthRepository

class ResetPasswordUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Result.failure(DomainError.ValidationError("Email is required"))
        }
        return runSuspendCatching {
            authRepository.resetPassword(trimmedEmail)
        }
    }
}
