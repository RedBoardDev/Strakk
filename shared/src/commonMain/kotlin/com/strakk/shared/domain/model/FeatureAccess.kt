package com.strakk.shared.domain.model

sealed interface FeatureAccess {
    data object Granted : FeatureAccess
    data class Gated(val feature: ProFeature) : FeatureAccess
}
