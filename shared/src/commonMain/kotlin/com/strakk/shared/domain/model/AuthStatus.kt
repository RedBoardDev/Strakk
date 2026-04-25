package com.strakk.shared.domain.model

/**
 * Represents the current authentication state of the user.
 *
 * Emitted by [com.strakk.shared.domain.repository.AuthRepository.observeSessionStatus].
 */
sealed interface AuthStatus {

    /** Session state is being determined (app launch, token refresh). */
    data object Loading : AuthStatus

    /** No valid session exists. User must authenticate. */
    data object Unauthenticated : AuthStatus

    /**
     * A valid Supabase session exists.
     *
     * @param hasProfile Whether a `profiles` row exists for this user.
     *   `true` = returning user (go to Home),
     *   `false` = new user (go to Onboarding).
     */
    data class Authenticated(
        val hasProfile: Boolean,
    ) : AuthStatus
}
