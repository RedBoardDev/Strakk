package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import kotlinx.coroutines.flow.Flow

/**
 * Supabase-backed repository for the `meals` and `meal_entries` tables,
 * and for the AI extraction edge functions.
 *
 * Follows the same reactive cache pattern as [NutritionRepository]:
 * [observeMealsForDate] emits immediately from cache and re-fetches on first
 * subscription per date.
 */
interface MealRepository {

    // --- Observation ---

    /**
     * Reactive stream of all [Meal] containers for the given [date], ordered
     * by creation time ascending.  Includes nested [MealEntry] items via
     * PostgREST embed.
     */
    fun observeMealsForDate(date: String): Flow<List<Meal>>

    /**
     * Reactive stream for a single [Meal] by [id].
     * Emits null if the meal has been deleted or is not in cache.
     */
    fun observeMeal(id: String): Flow<Meal?>

    // --- Mutations ---

    /**
     * Creates a new Meal container in Supabase.
     *
     * @param name Initial meal name (e.g. "Déjeuner - 12:45").
     * @param date ISO-8601 date string ("yyyy-MM-dd").
     */
    suspend fun createMeal(name: String, date: String): Meal

    /** Renames the meal with the given [id]. */
    suspend fun renameMeal(id: String, name: String)

    /**
     * Deletes the meal and all its child entries (via DB CASCADE).
     * Also removes associated Storage objects for entries with `photo_path`.
     */
    suspend fun deleteMeal(id: String)

    /**
     * Commits a draft: inserts a `meals` row then inserts all resolved entries.
     *
     * [photoPathsByItemId] maps draft item IDs to their Supabase Storage paths
     * for items that had photos uploaded.
     *
     * @return The committed [Meal] with all entries populated.
     */
    suspend fun commitMealDraft(
        draftId: String,
        name: String,
        date: String,
        entries: List<DraftItem.Resolved>,
        photoPathsByItemId: Map<String, String>,
    ): Meal

    /** Adds a single entry to an already-committed (Processed) meal. */
    suspend fun addEntryToMeal(mealId: String, entry: DraftItem.Resolved)

    /** Clears all in-memory caches. Should be called on sign-out. */
    fun clearCache()

    // --- AI extraction via edge functions ---

    /**
     * Input descriptor for a photo item in a batch extraction request.
     *
     * @param id Draft item ID (echoed back in results for deterministic mapping).
     * @param photoPath Supabase Storage path (already uploaded).
     * @param hint Optional user hint to guide Claude.
     */
    data class ExtractPhotoItem(
        val id: String,
        val photoPath: String,
        val hint: String?,
    )

    /**
     * Input descriptor for a text item in a batch extraction request.
     */
    data class ExtractTextItem(
        val id: String,
        val description: String,
    )

    /**
     * Result of a single item extraction.
     * [resolvedItem] is null when the item could not be analyzed.
     */
    data class ExtractItemResult(
        val id: String,
        val resolvedItem: DraftItem.Resolved?,
        val error: String?,
    )

    /**
     * Calls the `extract-meal-draft` edge function with one batch of items.
     *
     * Max 2 [photoItems] per call (split enforced by [ProcessMealDraftUseCase]).
     */
    suspend fun extractMealDraftBatch(
        draftId: String,
        photoItems: List<ExtractPhotoItem>,
        textItems: List<ExtractTextItem>,
    ): List<ExtractItemResult>

    /**
     * Analyzes a single photo via `analyze-meal-single` using the original
     * base64 payload. Used as the per-item fallback when a batch call fails —
     * we reuse the base64 kept locally in the Draft instead of re-downloading
     * the uploaded Storage copy.
     *
     * @param imageBase64 Compressed JPEG base64 (no data: prefix).
     * @param hint Optional user hint.
     * @param draftItemId Draft item ID; becomes the [MealEntry.id] of the result.
     */
    suspend fun analyzePhotoSingle(
        imageBase64: String,
        hint: String?,
        draftItemId: String,
    ): DraftItem.Resolved

    /**
     * Analyzes a single text description via `analyze-meal-single`.
     * Used as the per-item fallback when a batch call fails.
     */
    suspend fun analyzeTextSingle(
        description: String,
        draftItemId: String,
    ): DraftItem.Resolved

    /**
     * Analyzes a photo for a quick-add flow (outside of a Draft).
     *
     * @return A fresh [MealEntry] with [MealEntry.source] = [EntrySource.PhotoAi],
     *   ready to be persisted via [NutritionRepository.addMeal].
     *   The [MealEntry.id] is empty — the DB generates one on insert.
     */
    suspend fun analyzePhotoForQuickAdd(
        imageBase64: String,
        hint: String?,
        logDate: String,
    ): MealEntry

    /**
     * Analyzes a text description for a quick-add flow.
     *
     * @return A fresh [MealEntry] with [MealEntry.source] = [EntrySource.TextAi].
     */
    suspend fun analyzeTextForQuickAdd(
        description: String,
        logDate: String,
    ): MealEntry
}
