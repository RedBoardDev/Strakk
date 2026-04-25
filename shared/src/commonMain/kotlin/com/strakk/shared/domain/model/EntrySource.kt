package com.strakk.shared.domain.model

/**
 * Origin of a [MealEntry].
 *
 * Matches the `CHECK` constraint on `meal_entries.source` in the DB schema.
 * [fromString] is tolerant to unknown values — it falls back to [Manual].
 */
enum class EntrySource {
    Search,
    Barcode,
    Manual,
    TextAi,
    PhotoAi,
    Frequent,
    ;

    companion object {
        /** Converts a DB string to an [EntrySource], defaulting to [Manual] on unknown values. */
        fun fromString(value: String): EntrySource = when (value) {
            "search" -> Search
            "barcode" -> Barcode
            "manual" -> Manual
            "text_ai" -> TextAi
            "photo_ai" -> PhotoAi
            "frequent" -> Frequent
            else -> Manual
        }
    }
}

/** Returns the DB-persisted string representation of this [EntrySource]. */
fun EntrySource.toDbString(): String = when (this) {
    EntrySource.Search -> "search"
    EntrySource.Barcode -> "barcode"
    EntrySource.Manual -> "manual"
    EntrySource.TextAi -> "text_ai"
    EntrySource.PhotoAi -> "photo_ai"
    EntrySource.Frequent -> "frequent"
}
