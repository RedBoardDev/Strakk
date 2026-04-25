package com.strakk.shared.domain.model

/**
 * A single meal or food item logged for a given day.
 *
 * An entry may be orphaned ([mealId] = null, i.e. a quick-add) or attached
 * to a [Meal] container ([mealId] != null).
 *
 * Dates are stored as ISO-8601 strings ("yyyy-MM-dd") to avoid
 * cross-platform serialization complexity.
 */
data class MealEntry(
    val id: String,
    val logDate: String,
    val name: String?,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    val source: EntrySource,
    val createdAt: String,
    /** Null for quick-add orphan entries; non-null when attached to a [Meal]. */
    val mealId: String? = null,
    /** Textual quantity description, e.g. "150g", "1 bowl". Null when not specified. */
    val quantity: String? = null,
    /**
     * Sub-items breakdown from a photo AI analysis.
     * Non-null only for [EntrySource.PhotoAi] entries.
     * Stored as `breakdown_json` JSONB on the DB.
     */
    val breakdown: List<BreakdownItem>? = null,
    /**
     * Supabase Storage path of the attached photo, e.g.
     * `{userId}/{mealId}/{entryId}.jpg`. Non-null only for [EntrySource.PhotoAi].
     * Used by [com.strakk.shared.domain.repository.MealRepository.deleteMeal]
     * to clean up orphan objects and by the UI to build signed URLs.
     */
    val photoPath: String? = null,
)
