package com.strakk.shared.data.remote

import com.strakk.shared.domain.common.DomainError
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Resolves the current authenticated user id from the Supabase session.
 *
 * Prefers [auth.currentUserOrNull]. Falls back to decoding the JWT access
 * token payload's `sub` claim — necessary on cold start when the user object
 * is not yet populated in the session state but a valid token exists.
 *
 * Throws [DomainError.AuthError] if no authenticated session is available.
 */
internal class CurrentUserIdProvider(
    private val supabaseClient: SupabaseClient,
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun currentOrThrow(): String {
        supabaseClient.auth.currentUserOrNull()?.id?.let { return it }

        val accessToken = supabaseClient.auth.currentSessionOrNull()?.accessToken
            ?: throw DomainError.AuthError("No authenticated session. Please sign in again.")

        val payload = accessToken.split(".").getOrNull(1)
            ?: throw DomainError.AuthError("Invalid session token format.")

        val decoded = try {
            Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                .decode(payload)
                .decodeToString()
        } catch (e: Exception) {
            throw DomainError.AuthError("Failed to decode session token.", e)
        }

        return Json.parseToJsonElement(decoded)
            .jsonObject["sub"]
            ?.jsonPrimitive
            ?.content
            ?: throw DomainError.AuthError("Session token is missing user identity ('sub' claim).")
    }
}
