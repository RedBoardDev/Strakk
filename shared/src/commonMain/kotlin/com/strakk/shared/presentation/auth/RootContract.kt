package com.strakk.shared.presentation.auth

sealed interface RootUiState {
    data object Loading : RootUiState
    data object Unauthenticated : RootUiState
    data class Authenticated(
        val onboardingCompleted: Boolean,
        val showTrialExpiredModal: Boolean = false,
    ) : RootUiState
}
