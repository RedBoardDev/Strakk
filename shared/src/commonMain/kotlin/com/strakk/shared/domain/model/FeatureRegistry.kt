package com.strakk.shared.domain.model

object FeatureRegistry {
    private val metadata: Map<Feature, FeatureMetadata> = mapOf(
        Feature.AI_PHOTO_ANALYSIS to FeatureMetadata(
            feature = Feature.AI_PHOTO_ANALYSIS,
            titleKey = "feature_ai_photo_title",
            descriptionKey = "feature_ai_photo_description",
            iconIos = "camera.fill",
            iconAndroid = "CameraAlt",
        ),
        Feature.AI_TEXT_ANALYSIS to FeatureMetadata(
            feature = Feature.AI_TEXT_ANALYSIS,
            titleKey = "feature_ai_text_title",
            descriptionKey = "feature_ai_text_description",
            iconIos = "text.bubble.fill",
            iconAndroid = "TextFields",
        ),
        Feature.AI_WEEKLY_SUMMARY to FeatureMetadata(
            feature = Feature.AI_WEEKLY_SUMMARY,
            titleKey = "feature_ai_summary_title",
            descriptionKey = "feature_ai_summary_description",
            iconIos = "chart.bar.fill",
            iconAndroid = "BarChart",
        ),
        Feature.HEALTH_SYNC to FeatureMetadata(
            feature = Feature.HEALTH_SYNC,
            titleKey = "feature_health_sync_title",
            descriptionKey = "feature_health_sync_description",
            iconIos = "heart.fill",
            iconAndroid = "MonitorHeart",
        ),
        Feature.UNLIMITED_HISTORY to FeatureMetadata(
            feature = Feature.UNLIMITED_HISTORY,
            titleKey = "feature_unlimited_history_title",
            descriptionKey = "feature_unlimited_history_description",
            iconIos = "clock.fill",
            iconAndroid = "History",
        ),
        Feature.PHOTO_COMPARISON to FeatureMetadata(
            feature = Feature.PHOTO_COMPARISON,
            titleKey = "feature_photo_comparison_title",
            descriptionKey = "feature_photo_comparison_description",
            iconIos = "photo.on.rectangle.angled",
            iconAndroid = "Compare",
        ),
        Feature.HEVY_EXPORT to FeatureMetadata(
            feature = Feature.HEVY_EXPORT,
            titleKey = "feature_hevy_export_title",
            descriptionKey = "feature_hevy_export_description",
            iconIos = "dumbbell.fill",
            iconAndroid = "Upload",
        ),
    )

    fun get(feature: Feature): FeatureMetadata = metadata[feature] ?: error("No metadata for feature: $feature")

    fun all(): List<FeatureMetadata> = Feature.entries.map { get(it) }
}
