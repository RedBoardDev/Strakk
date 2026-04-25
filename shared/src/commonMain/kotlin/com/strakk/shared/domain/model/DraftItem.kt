package com.strakk.shared.domain.model

/**
 * An item inside an [ActiveMealDraft].
 *
 * Items are either already resolved (macros known) or pending AI extraction
 * (photo or free-form text).
 *
 * Serialization lives in the data layer only — [DraftItem] is pure domain.
 */
sealed interface DraftItem {
    val id: String

    /**
     * An item already assigned nutritional values (barcode, search, manual, or
     * a previously-analyzed entry).
     */
    data class Resolved(
        override val id: String,
        val entry: MealEntry,
    ) : DraftItem

    /**
     * A photo pending AI extraction.
     *
     * [imageBase64] holds the compressed JPEG (≤ 300 KB, 1024×1024) inline in
     * the Draft JSON until commit.  The photo is uploaded to Supabase Storage
     * at commit time, not at addition time (cf. D18 / §7.4).
     */
    data class PendingPhoto(
        override val id: String,
        val imageBase64: String,
        val hint: String?,
    ) : DraftItem

    /**
     * A free-form text description pending AI extraction.
     */
    data class PendingText(
        override val id: String,
        val description: String,
    ) : DraftItem
}
