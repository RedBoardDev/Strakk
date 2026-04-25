package com.strakk.shared.data.repository

import com.strakk.shared.data.remote.CurrentUserIdProvider
import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.Logger
import com.strakk.shared.domain.repository.MealPhotoRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private const val LOG_TAG = "MealPhotoRepository"
private const val BUCKET = "meal-photos"
private val BACKOFF_MS = longArrayOf(200L, 800L, 2000L)

/**
 * Supabase Storage implementation of [MealPhotoRepository].
 *
 * Enforces the path convention `{userId}/{draftId}/{itemId}.jpg` so that the
 * bucket RLS policy (`(storage.foldername(name))[1] = auth.uid()::text`) is
 * respected end-to-end.
 */
internal class MealPhotoRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val userIdProvider: CurrentUserIdProvider,
    private val logger: Logger,
) : MealPhotoRepository {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun uploadPhoto(
        draftId: String,
        itemId: String,
        base64: String,
    ): String {
        val userId = userIdProvider.currentOrThrow()
        val path = "$userId/$draftId/$itemId.jpg"
        val bytes = try {
            Base64.decode(base64)
        } catch (e: Exception) {
            throw DomainError.DataError("Invalid base64 image payload", e)
        }

        val bucket = supabaseClient.storage.from(BUCKET)
        var lastError: Throwable? = null

        repeat(BACKOFF_MS.size + 1) { attempt ->
            if (attempt > 0) delay(BACKOFF_MS[attempt - 1])
            try {
                bucket.upload(path, bytes) { upsert = true }
                return path
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                logger.e(LOG_TAG, "upload attempt ${attempt + 1} failed for $path", e)
            }
        }

        throw DomainError.DataError(
            "Photo upload failed after ${BACKOFF_MS.size + 1} attempts",
            lastError,
        )
    }

    override suspend fun getSignedUrl(path: String, ttlSeconds: Long): String =
        supabaseClient.storage
            .from(BUCKET)
            .createSignedUrl(path, ttlSeconds.seconds)

    override suspend fun deletePhoto(path: String) {
        try {
            supabaseClient.storage.from(BUCKET).delete(path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "deletePhoto failed for $path", e)
        }
    }

    override suspend fun deletePhotos(paths: List<String>) {
        if (paths.isEmpty()) return
        try {
            supabaseClient.storage.from(BUCKET).delete(paths)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(LOG_TAG, "deletePhotos failed for ${paths.size} paths", e)
        }
    }
}
