package com.strakk.shared.domain.common

/**
 * Sealed hierarchy of domain-level errors.
 *
 * Used as [Result.failure] payloads across use case boundaries.
 * Each variant carries a human-readable [message] and an optional [cause].
 */
sealed class DomainError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * Authentication-related failure (expired session, invalid token, network error during auth).
     */
    class AuthError(
        message: String,
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    /**
     * Data-layer failure (network, database, serialization).
     */
    class DataError(
        message: String,
        cause: Throwable? = null,
    ) : DomainError(message, cause)

    /**
     * Client-side validation failure (bad input format, missing required field).
     */
    class ValidationError(
        message: String,
    ) : DomainError(message)
}
