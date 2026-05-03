package com.strakk.shared.presentation.auth

sealed interface AuthFlowUiState {
    data object Welcome : AuthFlowUiState
    data class SignIn(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
    ) : AuthFlowUiState
    data class SignUp(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
    ) : AuthFlowUiState
}

sealed interface AuthFlowEvent {
    data object OnContinueWithEmail : AuthFlowEvent
    data class OnEmailChanged(val email: String) : AuthFlowEvent
    data class OnPasswordChanged(val password: String) : AuthFlowEvent
    data object OnSignIn : AuthFlowEvent
    data object OnSignUp : AuthFlowEvent
    data object OnSwitchToSignUp : AuthFlowEvent
    data object OnSwitchToSignIn : AuthFlowEvent
}
