package com.strakk.shared.domain.model

import kotlinx.datetime.Instant

/**
 * The single active meal draft being composed locally.
 *
 * A draft is local-only until [CommitMealDraftUseCase] inserts it into
 * Supabase.  It is persisted in `multiplatform-settings` under the key
 * `active_meal_draft` so that it survives app restarts.
 *
 * Only one draft can be active at a time (enforced by [CreateMealDraftUseCase]).
 *
 * @param uploadedPaths Storage paths already successfully uploaded for pending
 *   photos.  Key = [DraftItem.id], value = Storage path.  Populated during
 *   the upload phase of [ProcessMealDraftUseCase] and preserved across retries
 *   to avoid re-uploading photos already on Storage.  Cleared on commit or discard.
 */
data class ActiveMealDraft(
    val id: String,
    /** ISO-8601 date string ("yyyy-MM-dd"). */
    val date: String,
    val name: String,
    val createdAt: Instant,
    val items: List<DraftItem>,
    val uploadedPaths: Map<String, String> = emptyMap(),
)
