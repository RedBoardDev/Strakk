package com.strakk.shared.domain.model

data class ProFeatureInfo(
    val feature: ProFeature,
    val iconId: String,
    val title: String,
    val description: String,
)

fun allProFeatures(): List<ProFeatureInfo> = listOf(
    ProFeatureInfo(
        ProFeature.AI_PHOTO_ANALYSIS,
        "camera.ai",
        "Photo intelligente",
        "Prends une photo, l'IA calcule tes macros.",
    ),
    ProFeatureInfo(
        ProFeature.AI_TEXT_ANALYSIS,
        "text.ai",
        "Texte intelligent",
        "Décris ton repas, l'IA fait le reste.",
    ),
    ProFeatureInfo(
        ProFeature.AI_WEEKLY_SUMMARY,
        "chart.weekly",
        "Bilan hebdo IA",
        "Un résumé personnalisé chaque semaine.",
    ),
    ProFeatureInfo(
        ProFeature.HEALTH_SYNC,
        "heart.sync",
        "Sync Santé",
        "Connecte Apple Santé ou Google Fit.",
    ),
    ProFeatureInfo(
        ProFeature.UNLIMITED_HISTORY,
        "clock.history",
        "Historique illimité",
        "Accède à tout ton historique sans limite.",
    ),
    ProFeatureInfo(
        ProFeature.PHOTO_COMPARISON,
        "photo.compare",
        "Comparaison photo",
        "Compare tes photos check-in côte à côte.",
    ),
)
