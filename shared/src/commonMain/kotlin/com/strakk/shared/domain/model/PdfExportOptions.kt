package com.strakk.shared.domain.model

data class PdfExportOptions(
    val includePhotos: Boolean = true,
    val includeMeasurements: Boolean = true,
    val includeFeelings: Boolean = true,
    val includeProtein: Boolean = true,
    val includeCalories: Boolean = true,
    val includeCarbs: Boolean = true,
    val includeFat: Boolean = true,
    val includeWater: Boolean = true,
    val includeAverages: Boolean = true,
    val includeDailyData: Boolean = true,
    val includeAiSummary: Boolean = true,
) {
    /** True if at least one nutrition field is included. */
    val includeNutrition: Boolean
        get() = includeProtein || includeCalories || includeCarbs || includeFat || includeWater || includeAiSummary
}
