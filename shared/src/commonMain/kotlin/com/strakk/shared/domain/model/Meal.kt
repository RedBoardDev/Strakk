package com.strakk.shared.domain.model

import kotlinx.datetime.Instant

/**
 * A meal container grouping one or more [MealEntry] items.
 *
 * [status] is always [MealStatus.Processed] for rows returned from Supabase.
 * The [MealStatus.Draft] state is managed locally via [ActiveMealDraft].
 *
 * [entries] is populated via PostgREST embed (`select=*,meal_entries(*)`).
 * It may be empty if no entries have been added or if not fetched with embed.
 */
data class Meal(
    val id: String,
    val userId: String,
    /** ISO-8601 date string ("yyyy-MM-dd"). */
    val date: String,
    val name: String,
    val status: MealStatus,
    val createdAt: Instant,
    val entries: List<MealEntry>,
)
