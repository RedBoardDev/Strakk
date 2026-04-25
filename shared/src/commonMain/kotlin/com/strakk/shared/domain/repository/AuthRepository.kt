package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.AuthStatus
import kotlinx.coroutines.flow.Flow

/**
 * Authentication operations backed by Supabase Auth.
 *
 * Implementations live in the data layer and are `internal`.
 * Methods throw on failure — use cases wrap calls in [runCatching].
 */
interface AuthRepository {

    /**
     * Observes the current session status as a reactive stream.
     *
     * Emits [AuthStatus.Loading] initially, then [AuthStatus.Authenticated]
     * or [AuthStatus.Unauthenticated] as the session state changes.
     * The `hasProfile` flag in [AuthStatus.Authenticated] is NOT set here —
     * it is determined by the presentation layer via [ProfileRepository.profileExists].
     */
    fun observeSessionStatus(): Flow<AuthStatus>

    /**
     * Signs in an existing user with email and password.
     *
     * @param email The user's email address (must be validated before calling).
     * @param password The user's password.
     * @throws Exception on network, invalid credentials, or Supabase errors.
     */
    suspend fun signIn(email: String, password: String)

    /**
     * Creates a new account with email and password.
     *
     * @param email The email address for the new account (must be validated before calling).
     * @param password The password for the new account (minimum 6 characters).
     * @throws Exception on network or Supabase errors.
     */
    suspend fun signUp(email: String, password: String)

    /**
     * Signs out the current user and clears the local session.
     *
     * @throws Exception on network errors during server-side invalidation.
     */
    suspend fun signOut()

    /**
     * Returns the email address of the currently authenticated user.
     *
     * @return The email string, or `null` if no session exists.
     */
    suspend fun getCurrentUserEmail(): String?
}
