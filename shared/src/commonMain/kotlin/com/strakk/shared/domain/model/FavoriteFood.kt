package com.strakk.shared.domain.model

import kotlinx.datetime.Instant

/**
 * A user-favorited food template, denormalized from a [MealEntry] at the
 * moment it was hearted. Survives deletion of the original entry.
 *
 * One row per [normalizedName] per user (DB-enforced uniqueness).
 */
data class FavoriteFood(
    val id: String,
    val name: String,
    /** Lowercased + unaccented; used as the dedup key per user. */
    val normalizedName: String,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    /** Default quantity to suggest when re-adding (e.g. "150g"). */
    val quantity: String?,
    /** Catalog id if the favorite was created from a catalog item. */
    val foodCatalogId: Long? = null,
    val createdAt: Instant,
)
