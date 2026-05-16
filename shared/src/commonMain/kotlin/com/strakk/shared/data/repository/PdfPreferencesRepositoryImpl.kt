package com.strakk.shared.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.boolean
import com.strakk.shared.domain.model.PdfExportOptions
import com.strakk.shared.domain.repository.PdfPreferencesRepository

internal class PdfPreferencesRepositoryImpl(
    private val settings: Settings,
) : PdfPreferencesRepository {

    private var includePhotos by settings.boolean("pdf_pref_photos", true)
    private var includeMeasurements by settings.boolean("pdf_pref_measurements", true)
    private var includeFeelings by settings.boolean("pdf_pref_feelings", true)
    private var includeProtein by settings.boolean("pdf_pref_protein", true)
    private var includeCalories by settings.boolean("pdf_pref_calories", true)
    private var includeCarbs by settings.boolean("pdf_pref_carbs", true)
    private var includeFat by settings.boolean("pdf_pref_fat", true)
    private var includeWater by settings.boolean("pdf_pref_water", true)
    private var includeAverages by settings.boolean("pdf_pref_averages", true)
    private var includeDailyData by settings.boolean("pdf_pref_daily_data", true)
    private var includeTraining by settings.boolean("pdf_pref_training", true)

    override fun getExportOptions(): PdfExportOptions = PdfExportOptions(
        includePhotos = includePhotos,
        includeMeasurements = includeMeasurements,
        includeFeelings = includeFeelings,
        includeProtein = includeProtein,
        includeCalories = includeCalories,
        includeCarbs = includeCarbs,
        includeFat = includeFat,
        includeWater = includeWater,
        includeAverages = includeAverages,
        includeDailyData = includeDailyData,
        includeTraining = includeTraining,
    )

    override fun saveExportOptions(options: PdfExportOptions) {
        includePhotos = options.includePhotos
        includeMeasurements = options.includeMeasurements
        includeFeelings = options.includeFeelings
        includeProtein = options.includeProtein
        includeCalories = options.includeCalories
        includeCarbs = options.includeCarbs
        includeFat = options.includeFat
        includeWater = options.includeWater
        includeAverages = options.includeAverages
        includeDailyData = options.includeDailyData
        includeTraining = options.includeTraining
    }
}
