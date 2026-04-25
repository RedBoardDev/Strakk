package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.MealEntry

/**
 * Looks up product nutritional data from external sources (Open Food Facts).
 *
 * This is kept separate from [NutritionRepository] (Supabase data) because the
 * two sources have different error semantics: an OFF lookup that returns
 * "unknown product" is a legitimate empty result, not a failure.
 */
interface BarcodeLookupRepository {

    /**
     * Looks up [barcode] on Open Food Facts and returns a pre-filled
     * [MealEntry] with macros per 100g, or null if unknown.
     *
     * The returned entry has [MealEntry.source] = `Barcode`, empty id/createdAt
     * (server-generated on insert), and null `mealId` / `breakdown` / `photoPath`.
     */
    suspend fun lookup(barcode: String): MealEntry?
}
