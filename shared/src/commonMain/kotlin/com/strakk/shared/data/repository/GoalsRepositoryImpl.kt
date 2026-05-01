package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.CalculateGoalsResponseDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.mapper.toDto
import com.strakk.shared.domain.model.AiGoalsResult
import com.strakk.shared.domain.model.CalculateGoalsRequest
import com.strakk.shared.domain.repository.GoalsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class GoalsRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : GoalsRepository {

    override suspend fun calculateGoals(request: CalculateGoalsRequest): AiGoalsResult {
        val requestBody = json.encodeToString(request.toDto())

        val response = supabaseClient.functions.invoke(
            function = "calculate-goals",
            body = requestBody,
            headers = Headers.build {
                append(HttpHeaders.ContentType, "application/json")
            },
        )

        val responseBody = response.body<String>()
        val dto = json.decodeFromString<CalculateGoalsResponseDto>(responseBody)
        return dto.toDomain()
    }
}
