package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable form of [com.strakk.shared.domain.model.ActiveMealDraft],
 * persisted locally via `multiplatform-settings` under the key
 * `active_meal_draft`.
 *
 * This is never sent over the wire to Supabase — a draft lives only on the
 * current device until committed.
 */
@Serializable
internal data class ActiveMealDraftDto(
    val id: String,
    val date: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,
    val items: List<DraftItemDto>,
    @SerialName("uploaded_paths") val uploadedPaths: Map<String, String> = emptyMap(),
)

/**
 * Flat polymorphic representation of [com.strakk.shared.domain.model.DraftItem].
 *
 * `type` is the discriminator : `resolved`, `pending_photo`, or `pending_text`.
 * Only the fields relevant to [type] are populated.
 *
 * This shape is chosen over kotlinx.serialization's sealed-polymorphism because
 * we serialize locally only (no wire-format requirements) and a flat shape is
 * simpler to manipulate in migrations.
 */
@Serializable
internal data class DraftItemDto(
    val id: String,
    val type: String,
    val entry: MealEntryDto? = null,
    @SerialName("image_base64") val imageBase64: String? = null,
    val hint: String? = null,
    val description: String? = null,
)
