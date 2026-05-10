package com.strakk.shared.domain.model

/**
 * A food item from the shared catalogue (CIQUAL generic + OFF FR brands).
 *
 * Nutritional values are per 100 g.
 */
data class FoodCatalogItem(
    val id: Long,
    val source: FoodCatalogSource,
    val name: String,
    val brand: String?,
    val protein: Double,
    val calories: Double,
    val fat: Double?,
    val carbs: Double?,
    /** Default portion in grams for display purposes (e.g. 60 for 1 petit-suisse, 100 fallback). */
    val defaultPortionGrams: Double,
    /** Human-readable serving label, e.g. "1 pot", "1 tranche". Null when only grams make sense. */
    val servingLabel: String?,
    /** Nutri-Score "a".."e" (single lowercase letter), null when unknown. */
    val nutriscore: String?,
    /** NOVA processing group 1..4, null when unknown. */
    val novaGroup: Int?,
    /** EAN-13 barcode for OFF-sourced items, null for CIQUAL generics. */
    val barcode: String?,
    /** Image URL (typically Open Food Facts CDN) when available. */
    val imageUrl: String?,
)

enum class FoodCatalogSource {
    /** ANSES CIQUAL — generic French foods, scientific reference, no brand. */
    Ciqual,

    /** Open Food Facts — pre-seeded subset of French branded products. */
    OffFr,

    /** Open Food Facts — fetched live and cached on demand. */
    OffLive,

    /** Manually curated by an admin. */
    ManualAdmin,

    /** USDA FoodData Central — US reference database (SR Legacy). */
    Usda,

    /** USDA Foundation Foods — curated subset with more precise nutrient data. */
    UsdaFoundation,

    /** USDA Survey (FNDDS) — composite dishes (pizza, stews, fried rice, etc.). */
    UsdaFndds,

    /** USDA Branded Foods — brand-specific products (Coca-Cola, Barilla, etc.). */
    UsdaBranded,
}

/** Returns the DB-persisted string representation of this [FoodCatalogSource]. */
fun FoodCatalogSource.toDbString(): String = when (this) {
    FoodCatalogSource.Ciqual -> "ciqual"
    FoodCatalogSource.OffFr -> "off_fr"
    FoodCatalogSource.OffLive -> "off_live"
    FoodCatalogSource.ManualAdmin -> "manual_admin"
    FoodCatalogSource.Usda -> "usda"
    FoodCatalogSource.UsdaFoundation -> "usda_foundation"
    FoodCatalogSource.UsdaFndds -> "usda_fndds"
    FoodCatalogSource.UsdaBranded -> "usda_branded"
}
