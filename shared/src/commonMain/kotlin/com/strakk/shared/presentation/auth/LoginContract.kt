package com.strakk.shared.presentation.auth

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface LoginEvent {
    data class OnEmailChanged(val email: String) : LoginEvent
    data class OnPasswordChanged(val password: String) : LoginEvent
    data object OnLogin : LoginEvent
    data object OnForgotPassword : LoginEvent
    data object OnNavigateToSignUp : LoginEvent
}

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToOnboarding : LoginEffect
    data object NavigateToSignUp : LoginEffect
    data class ShowMessage(val message: String) : LoginEffect
}
