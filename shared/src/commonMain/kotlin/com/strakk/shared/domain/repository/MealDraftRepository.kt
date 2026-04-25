package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.MealEntry
import kotlinx.coroutines.flow.Flow

/**
 * Local-only repository for the single active [ActiveMealDraft].
 *
 * Backed by `multiplatform-settings` (NSUserDefaults on iOS,
 * SharedPreferences on Android).  No network I/O.
 *
 * All mutations persist immediately to settings and update the returned [Flow].
 */
interface MealDraftRepository {

    /**
     * Reactive stream of the current draft, or null if no draft is active.
     * Emits immediately from the persisted state on first subscription.
     */
    fun observeActiveDraft(): Flow<ActiveMealDraft?>

    /**
     * Creates a new draft and makes it the active one.
     * Replaces any existing draft without warning (call-sites should check first).
     *
     * @param name Initial display name (e.g. "Repas - 12:45").
     * @param date ISO-8601 date string ("yyyy-MM-dd").
     */
    suspend fun createDraft(name: String, date: String): ActiveMealDraft

    /** Appends [item] to the current draft's item list. */
    suspend fun addItem(item: DraftItem)

    /** Removes the item with [itemId] from the current draft. */
    suspend fun removeItem(itemId: String)

    /** Updates the draft's display [name]. */
    suspend fun rename(name: String)

    /** Discards the current draft and clears all persisted state. */
    suspend fun discard()

    /**
     * Replaces a [DraftItem.PendingPhoto] or [DraftItem.PendingText] with a
     * [DraftItem.Resolved] carrying the extracted [entry].
     */
    suspend fun markItemResolved(itemId: String, entry: MealEntry)

    /**
     * Records the Supabase Storage path for an uploaded photo item.
     * Used to avoid re-uploading on retry.
     *
     * @param itemId Draft item ID (the [DraftItem] whose photo was uploaded).
     * @param path Storage path, e.g. `{userId}/{draftId}/{itemId}.jpg`.
     */
    suspend fun recordUploadedPath(itemId: String, path: String)
}
