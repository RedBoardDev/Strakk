package com.strakk.shared.domain.model

/**
 * Lifecycle status of a [Meal] container.
 *
 * - [Draft] : the meal is being composed locally. AI ingestion allowed.
 *   In practice, a Draft never lives on the server — it is persisted in
 *   [ActiveMealDraft] via multiplatform-settings until commit.
 * - [Processed] : the meal has been committed to Supabase. Only 0-call
 *   entry sources (search, barcode, manual) may be added post-commit.
 *
 * Note: rows returned from Supabase are **always** [Processed]. The enum
 * exists so the domain layer can represent both states in a single type.
 */
enum class MealStatus {
    Draft,
    Processed,
}
