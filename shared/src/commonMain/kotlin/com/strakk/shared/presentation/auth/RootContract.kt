package com.strakk.shared.presentation.auth

/**
 * Root-level authentication state.
 *
 * Observed by the app shell (Activity / SwiftUI root) to decide
 * which navigation graph to display.
 */
sealed interface RootUiState {
    /** Session state is being determined. */
    data object Loading : RootUiState

    /** No valid session — show auth flow. */
    data object Unauthenticated : RootUiState

    /**
     * Valid session exists.
     *
     * @param hasProfile `true` = go to Home, `false` = go to Onboarding.
     */
    data class Authenticated(
        val hasProfile: Boolean,
    ) : RootUiState
}
