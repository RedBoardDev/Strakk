package com.strakk.shared.data.datasource

import com.strakk.shared.data.dto.FoodCatalogItemDto
import com.strakk.shared.data.remote.SupabaseConfig
import com.strakk.shared.domain.common.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Calls the `search-off-live` Edge Function via raw Ktor — bypassing
 * `supabaseClient.functions.invoke()` which wraps non-2xx responses in
 * opaque exceptions that hide the actual server error body.
 */
internal class OffLiveSearchDataSource(
    private val supabaseClient: SupabaseClient,
    private val httpClient: HttpClient,
    private val logger: Logger,
) {

    @Serializable
    private data class Request(val q: String, val limit: Int)

    @Serializable
    private data class Response(val items: List<FoodCatalogItemDto> = emptyList())

    private val url = "${SupabaseConfig.URL}/functions/v1/$FUNCTION_NAME"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(query: String, limit: Int): List<FoodCatalogItemDto> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val token = supabaseClient.auth.currentSessionOrNull()?.accessToken
        if (token.isNullOrBlank()) {
            logger.e(LOG_TAG, "no auth token — skipping live search")
            return emptyList()
        }

        return try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                    append("apikey", SupabaseConfig.ANON_KEY)
                }
                setBody(Request(q = trimmed, limit = limit))
            }
            val status = response.status.value
            val rawBody = response.bodyAsText()
            if (status !in 200..299) {
                logger.e(LOG_TAG, "search-off-live HTTP $status: $rawBody")
                return emptyList()
            }
            json.decodeFromString(Response.serializer(), rawBody).items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "search-off-live failed: ${e::class.simpleName} ${e.message}")
            emptyList()
        }
    }

    private companion object {
        const val FUNCTION_NAME = "search-off-live"
        const val LOG_TAG = "OffLiveSearch"
    }
}
