package com.strakk.shared.data.remote

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
 * Throws [IllegalStateException] if no authenticated session is available.
 */
internal class CurrentUserIdProvider(
    private val supabaseClient: SupabaseClient,
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun currentOrThrow(): String {
        supabaseClient.auth.currentUserOrNull()?.id?.let { return it }

        val accessToken = supabaseClient.auth.currentSessionOrNull()?.accessToken
            ?: error("No authenticated session")

        val payload = accessToken.split(".").getOrNull(1)
            ?: error("Invalid JWT format")

        val decoded = Base64.UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
            .decode(payload)
            .decodeToString()

        return Json.parseToJsonElement(decoded)
            .jsonObject["sub"]
            ?.jsonPrimitive
            ?.content
            ?: error("No 'sub' claim in JWT")
    }
}
