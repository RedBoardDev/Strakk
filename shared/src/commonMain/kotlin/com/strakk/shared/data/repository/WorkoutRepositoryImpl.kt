package com.strakk.shared.data.repository

import com.strakk.shared.data.dto.ExportToHevyRequestDto
import com.strakk.shared.data.dto.ExportToHevyResponseDto
import com.strakk.shared.data.dto.ParseWorkoutPdfRequestDto
import com.strakk.shared.data.dto.ParseWorkoutPdfResponseDto
import com.strakk.shared.data.mapper.toDomain
import com.strakk.shared.data.mapper.toDto
import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.model.HevyExportResult
import com.strakk.shared.domain.model.WorkoutProgram
import com.strakk.shared.domain.model.WorkoutSession
import com.strakk.shared.domain.repository.WorkoutRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlin.coroutines.cancellation.CancellationException

private const val LOG_TAG = "WorkoutRepository"

/**
 * Supabase-backed implementation of [WorkoutRepository].
 *
 * Delegates to the `parse-workout-pdf` and `export-to-hevy` Edge Functions.
 * Always validates and refreshes the session before each call.
 */
internal class WorkoutRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val logger: Logger,
) : WorkoutRepository {

    override suspend fun parseWorkoutPdf(pdfBase64: String): WorkoutProgram {
        requireActiveSession()

        val response = try {
            supabaseClient.functions.invoke(
                function = "parse-workout-pdf",
                body = ParseWorkoutPdfRequestDto(pdfBase64 = pdfBase64),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "parse-workout-pdf invoke failed", e)
            throw DomainError.DataError(messageForInvokeError(e), e)
        }

        if (response.status.value !in 200..299) {
            throw DomainError.DataError(messageForHttpStatus(response.status.value, "parse-workout-pdf"))
        }

        return try {
            response.body<ParseWorkoutPdfResponseDto>().toDomain()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DataError("The PDF parser returned an unexpected result.", e)
        }
    }

    override suspend fun exportSessionToHevy(session: WorkoutSession): HevyExportResult {
        requireActiveSession()

        val response = try {
            supabaseClient.functions.invoke(
                function = "export-to-hevy",
                body = ExportToHevyRequestDto(session = session.toDto()),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "export-to-hevy invoke failed", e)
            throw DomainError.DataError(messageForInvokeError(e), e)
        }

        if (response.status.value !in 200..299) {
            throw DomainError.DataError(messageForHttpStatus(response.status.value, "export-to-hevy"))
        }

        return try {
            response.body<ExportToHevyResponseDto>().toDomain()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw DomainError.DataError("The Hevy export returned an unexpected result.", e)
        }
    }

    /**
     * Checks for an active session and proactively refreshes the token.
     * Mirrors the pattern from [NutritionRepositoryImpl.analyzeMeal].
     */
    private suspend fun requireActiveSession() {
        supabaseClient.auth.currentSessionOrNull()
            ?: throw DomainError.AuthError("No active session. Please sign in again.")

        try {
            supabaseClient.auth.refreshCurrentSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "Session refresh failed", e)
            throw DomainError.AuthError(
                "Your session expired. Please sign out and sign in again.",
                e,
            )
        }
    }

    private fun messageForInvokeError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            "401" in msg -> "Your session expired. Please sign in again."
            "400" in msg -> "The request was invalid. Please try again."
            "502" in msg || "503" in msg || "504" in msg ->
                "The service is temporarily unavailable. Try again."
            "500" in msg -> "The server errored. Check the server configuration."
            else -> "Request failed. Check your connection and try again."
        }
    }

    private fun messageForHttpStatus(status: Int, function: String): String = when (status) {
        401 -> "Your session expired. Please sign in again."
        400 -> "The request was invalid ($function)."
        500 -> "The server errored ($function)."
        502, 503, 504 -> "The service is temporarily unavailable. Try again."
        else -> "$function failed (HTTP $status)."
    }
}
