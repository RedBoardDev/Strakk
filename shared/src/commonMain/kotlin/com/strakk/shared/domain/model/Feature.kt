package com.strakk.shared.domain.model

enum class Feature(val key: String) {
    AI_PHOTO_ANALYSIS("ai_photo_analysis"),
    AI_TEXT_ANALYSIS("ai_text_analysis"),
    AI_WEEKLY_SUMMARY("ai_weekly_summary"),
    HEALTH_SYNC("health_sync"),
    UNLIMITED_HISTORY("unlimited_history"),
    PHOTO_COMPARISON("photo_comparison"),
    HEVY_EXPORT("hevy_export"),
    ;

    companion object {
        fun fromKey(key: String): Feature? = entries.find { it.key == key }
    }
}
