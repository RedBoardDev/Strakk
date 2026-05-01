package com.strakk.shared.domain.common

/**
 * Sealed hierarchy of domain-level errors.
 *
 * Used as [Result.failure] payloads across use case boundaries.
 * Each variant carries a human-readable [message] and an optional [cause].
 *
 * Implements [sealed interface] per domain conventions. Each subclass extends
 * [Exception] directly so that structured error handling and stack traces work
 * correctly across the call stack.
 */
sealed interface DomainError {

    val message: String
    val cause: Throwable?

    /**
     * Authentication-related failure (no session, expired session, invalid token).
     */
    data class AuthError(
        override val message: String,
        override val cause: Throwable? = null,
    ) : Exception(message, cause), DomainError

    /**
     * Data-layer failure (network, database, serialization).
     */
    data class DataError(
        override val message: String,
        override val cause: Throwable? = null,
    ) : Exception(message, cause), DomainError

    /**
     * Client-side validation failure (bad input format, missing required field).
     */
    data class ValidationError(
        override val message: String,
        override val cause: Throwable? = null,
    ) : Exception(message, cause), DomainError
}
