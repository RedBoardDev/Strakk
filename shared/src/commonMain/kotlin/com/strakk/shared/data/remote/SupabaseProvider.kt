package com.strakk.shared.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout

/**
 * Creates and configures the application-wide [SupabaseClient].
 *
 * Credentials are read from [SupabaseConfig], which is auto-generated at build
 * time from `local.properties` (gitignored — never committed to source control).
 *
 * The Auth plugin is configured with a custom scheme/host so that
 * supabase-kt automatically parses magic link deep links of the form
 * `strakk://auth?...` without manual token extraction.
 *
 * Ktor's HttpTimeout plugin is configured with generous timeouts because some
 * Edge Functions (e.g. analyze-meal, which calls Claude Vision) can legitimately
 * take 10-30 seconds. The default 10s would cause false failures.
 */
internal object SupabaseProvider {

    private const val REQUEST_TIMEOUT_MS = 60_000L  // 60s total per request
    private const val SOCKET_TIMEOUT_MS = 60_000L   // 60s per socket read/write

    @OptIn(SupabaseInternal::class)
    fun createClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.ANON_KEY,
    ) {
        install(Auth) {
            scheme = "strakk"
            host = "auth"
        }
        install(Postgrest)
        install(Functions)
        install(Storage)

        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
                socketTimeoutMillis = SOCKET_TIMEOUT_MS
            }
        }
    }
}
