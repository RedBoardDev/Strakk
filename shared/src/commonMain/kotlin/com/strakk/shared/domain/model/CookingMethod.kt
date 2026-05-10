package com.strakk.shared.domain.model

/**
 * Cooking method applied to a [GroundedMealItem].
 *
 * DB strings are lowercase variants of the enum names (e.g. "grilled").
 * Retention factors are post-MVP and not yet applied to macro computations.
 */
enum class CookingMethod {
    Grilled,
    Fried,
    Steamed,
    Baked,
    Boiled,
    Raw,
    Microwaved,
    ;

    /** Returns the DB-persisted string representation (lowercase enum name). */
    fun toDbString(): String = name.lowercase()

    companion object {
        /**
         * Converts a DB string to a [CookingMethod], returning null on unknown values.
         */
        fun fromString(value: String?): CookingMethod? =
            entries.firstOrNull { it.name.lowercase() == value?.lowercase() }
    }
}
