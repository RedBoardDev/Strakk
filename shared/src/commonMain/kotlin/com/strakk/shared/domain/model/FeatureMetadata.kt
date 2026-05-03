package com.strakk.shared.domain.model

data class FeatureMetadata(
    val feature: Feature,
    val titleKey: String,
    val descriptionKey: String,
    val iconIos: String,
    val iconAndroid: String,
)
