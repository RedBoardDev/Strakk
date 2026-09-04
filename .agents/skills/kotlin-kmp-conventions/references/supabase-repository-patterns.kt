package com.strakk.shared.data.repository

import com.strakk.shared.data.remote.dto.MealEntryDto
import com.strakk.shared.data.remote.dto.SessionDto
import com.strakk.shared.domain.error.DomainError
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.MealScanResult
import com.strakk.shared.domain.model.Session
import com.strakk.shared.domain.model.SessionId
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.SessionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// =============================================================================
// DTOs — @Serializable, internal, snake_case mapping
// =============================================================================

@Serializable
internal data class SessionDto(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String,
    @SerialName("exercise_count") val exerciseCount: Int,
)

@Serializable
internal data class MealEntryDto(
    val id: String,
    @SerialName("meal_type") val mealType: String,
    val calories: Int,
    @SerialName("protein_grams") val proteinGrams: Double,
    @SerialName("photo_url") val photoUrl: String?,
    @SerialName("logged_at") val loggedAt: String,
)

// =============================================================================
// Mappers — internal extension functions
// =============================================================================

internal fun SessionDto.toDomain(): Session = Session(
    id = SessionId(value = id),
    name = name,
    exerciseCount = exerciseCount,
)

internal fun MealEntryDto.toDomain(): MealEntry = MealEntry(
    id = id,
    mealType = mealType,
    calories = calories,
    proteinGrams = proteinGrams,
    photoUrl = photoUrl,
)

// =============================================================================
// Pattern 1: CRUD operations with supabase-kt
// =============================================================================

/**
 * Repository implementation — always internal.
 *
 * Maps Supabase exceptions to domain errors at this boundary.
 */
internal class SessionRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : SessionRepository {

    override fun getSessions(): Flow<List<Session>> = flow {
        val dtos = supabaseClient
            .from("sessions")
            .select()
            .decodeList<SessionDto>()

        emit(dtos.map { it.toDomain() })
    }

    override fun getSessionById(
        sessionId: SessionId,
    ): Flow<Session> = flow {
        val dto = supabaseClient
            .from("sessions")
            .select {
                filter {
                    eq("id", sessionId.value)
                }
            }
            .decodeSingle<SessionDto>()

        emit(dto.toDomain())
    }

    override suspend fun createSession(
        name: String,
        exercises: List<com.strakk.shared.domain.model.Exercise>,
    ): Result<Session> = runCatching {
        val dto = supabaseClient
            .from("sessions")
            .insert(
                buildJsonObject {
                    put("name", name)
                    put("exercise_count", exercises.size)
                },
            )
            .decodeSingle<SessionDto>()

        dto.toDomain()
    }.mapToDomainError()

    override suspend fun deleteSession(
        sessionId: SessionId,
    ): Result<Unit> = runCatching {
        supabaseClient
            .from("sessions")
            .delete {
                filter {
                    eq("id", sessionId.value)
                }
            }
    }.mapToDomainError()
}

// =============================================================================
// Pattern 2: Storage upload (for checkin photos)
// =============================================================================

internal class CheckinPhotoRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) {
    /**
     * Uploads a photo to Supabase Storage and returns the public URL.
     *
     * @param photoBytes The image bytes to upload.
     * @param fileName Unique file name (e.g., "checkin_2024-01-15_abc123.jpg").
     */
    suspend fun uploadCheckinPhoto(
        photoBytes: ByteArray,
        fileName: String,
    ): Result<String> = runCatching {
        val bucket = supabaseClient.storage.from("checkin-photos")

        bucket.upload(
            path = fileName,
            data = photoBytes,
        )

        bucket.publicUrl(fileName)
    }.mapToDomainError()
}

// =============================================================================
// Pattern 3: Edge Function call (for meal scan via AI)
// =============================================================================

@Serializable
internal data class MealScanResponseDto(
    val calories: Int,
    @SerialName("protein_grams") val proteinGrams: Double,
    @SerialName("carb_grams") val carbGrams: Double,
    @SerialName("fat_grams") val fatGrams: Double,
    val description: String,
)

internal fun MealScanResponseDto.toDomain(): MealScanResult = MealScanResult(
    calories = calories,
    proteinGrams = proteinGrams,
    carbGrams = carbGrams,
    fatGrams = fatGrams,
    description = description,
)

internal class MealRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : MealRepository {

    /**
     * Calls a Supabase Edge Function to analyze a meal photo.
     *
     * The Edge Function runs server-side AI inference and returns nutrition data.
     */
    suspend fun scanMealPhoto(
        photoBytes: ByteArray,
    ): Result<MealScanResult> = runCatching {
        val response = supabaseClient.functions.invoke(
            function = "scan-meal",
            body = photoBytes,
            headers = Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
            },
        )

        response.body<MealScanResponseDto>().toDomain()
    }.mapToDomainError()

    override suspend fun saveMealEntry(
        mealEntry: MealEntry,
    ): Result<Unit> = runCatching {
        supabaseClient
            .from("meal_entries")
            .insert(
                buildJsonObject {
                    put("meal_type", mealEntry.mealType)
                    put("calories", mealEntry.calories)
                    put("protein_grams", mealEntry.proteinGrams)
                },
            )
    }.mapToDomainError()

    override suspend fun refreshDailySummary(
        date: LocalDate,
    ): Result<Unit> = runCatching {
        supabaseClient.functions.invoke(
            function = "refresh-daily-summary",
            body = buildJsonObject {
                put("date", date.toString())
            },
        )
    }.mapToDomainError()
}

// =============================================================================
// Error mapping helper — converts exceptions to domain errors
// =============================================================================

/**
 * Maps any [Throwable] from the data layer to a [DomainError].
 *
 * This ensures no raw exceptions leak across the layer boundary.
 */
internal fun <T> Result<T>.mapToDomainError(): Result<T> = onFailure { throwable ->
    return Result.failure(
        DomainError.DataError(
            message = throwable.message ?: "Unknown error",
            cause = throwable,
        ),
    )
}
