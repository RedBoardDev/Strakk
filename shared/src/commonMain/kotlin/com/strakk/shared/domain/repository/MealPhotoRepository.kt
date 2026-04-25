package com.strakk.shared.domain.repository

/**
 * Supabase Storage repository for meal photos (bucket `meal-photos`).
 *
 * Path convention: `{userId}/{draftId}/{itemId}.jpg`
 */
interface MealPhotoRepository {

    /**
     * Uploads a base64-encoded JPEG to Supabase Storage.
     *
     * Retries up to 3 times with backoff (200ms, 800ms, 2s) before throwing.
     *
     * @param draftId The draft ID, used to build the Storage path.
     * @param itemId The draft item ID, used to build the Storage path.
     * @param base64 JPEG image encoded as a base64 string (no `data:` prefix).
     * @return The Storage path for the uploaded photo.
     */
    suspend fun uploadPhoto(draftId: String, itemId: String, base64: String): String

    /**
     * Returns a signed URL for the given Storage [path] with [ttlSeconds] TTL.
     *
     * @param path Storage path, e.g. `{userId}/{draftId}/{itemId}.jpg`.
     * @param ttlSeconds URL validity in seconds (default 300 = 5 min).
     */
    suspend fun getSignedUrl(path: String, ttlSeconds: Long = 300L): String

    /**
     * Removes a single object from Storage.  Best-effort — does not throw on 404.
     */
    suspend fun deletePhoto(path: String)

    /**
     * Removes multiple objects from Storage in one call.  Best-effort.
     */
    suspend fun deletePhotos(paths: List<String>)
}
