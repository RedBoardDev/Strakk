package com.strakk.shared.data.repository

import com.strakk.shared.domain.model.AuthStatus
import com.strakk.shared.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Supabase-backed implementation of [AuthRepository].
 *
 * Session status mapping:
 * - [SessionStatus.Authenticated] -> [AuthStatus.Authenticated] (hasProfile=false, enriched by VM)
 * - [SessionStatus.NotAuthenticated] -> [AuthStatus.Unauthenticated]
 * - [SessionStatus.Initializing] -> [AuthStatus.Loading]
 * - [SessionStatus.RefreshFailure] -> [AuthStatus.Unauthenticated]
 */
internal class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : AuthRepository {

    override fun observeSessionStatus(): Flow<AuthStatus> =
        supabaseClient.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthStatus.Authenticated(hasProfile = false)
                is SessionStatus.NotAuthenticated -> AuthStatus.Unauthenticated
                is SessionStatus.Initializing -> AuthStatus.Loading
                is SessionStatus.RefreshFailure -> AuthStatus.Unauthenticated
            }
        }

    override suspend fun signIn(email: String, password: String) {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String) {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        supabaseClient.auth.signOut()
    }

    override suspend fun resetPassword(email: String) {
        supabaseClient.auth.resetPasswordForEmail(email)
    }

    override suspend fun getCurrentUserEmail(): String? {
        supabaseClient.auth.currentUserOrNull()?.email?.let { return it }

        // Fallback: decode email from JWT access token (no network needed)
        val accessToken = supabaseClient.auth.currentSessionOrNull()?.accessToken
            ?: return null
        return try {
            @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
            val decoded = kotlin.io.encoding.Base64.UrlSafe
                .withPadding(kotlin.io.encoding.Base64.PaddingOption.ABSENT_OPTIONAL)
                .decode(accessToken.split(".")[1])
                .decodeToString()
            kotlinx.serialization.json.Json.parseToJsonElement(decoded)
                .jsonObject["email"]
                ?.jsonPrimitive
                ?.content
        } catch (_: Exception) {
            null
        }
    }
}
