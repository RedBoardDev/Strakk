package com.strakk.shared.presentation.auth

/**
 * Represents the current step of the auth flow.
 */
sealed interface AuthFlowUiState {

    /** Welcome screen — single "Continue with email" CTA. */
    data object Welcome : AuthFlowUiState

    /**
     * Sign-in screen.
     *
     * @param email Current email field value.
     * @param password Current password field value.
     * @param isLoading `true` while the sign-in request is in flight.
     * @param error Validation or network error message, `null` when no error.
     */
    data class SignIn(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
    ) : AuthFlowUiState

    /**
     * Sign-up screen.
     *
     * @param email Current email field value.
     * @param password Current password field value.
     * @param isLoading `true` while the sign-up request is in flight.
     * @param error Validation or network error message, `null` when no error.
     */
    data class SignUp(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
    ) : AuthFlowUiState
}

/** User interactions in the auth flow. */
sealed interface AuthFlowEvent {
    /** User taps "Continue with email" on the Welcome screen. */
    data object OnContinueWithEmail : AuthFlowEvent

    /** User types in the email field. */
    data class OnEmailChanged(val email: String) : AuthFlowEvent

    /** User types in the password field. */
    data class OnPasswordChanged(val password: String) : AuthFlowEvent

    /** User taps "Sign in" on the SignIn screen. */
    data object OnSignIn : AuthFlowEvent

    /** User taps "Create account" on the SignUp screen. */
    data object OnSignUp : AuthFlowEvent

    /** User taps "Create account" link on the SignIn screen. */
    data object OnSwitchToSignUp : AuthFlowEvent

    /** User taps "Sign in" link on the SignUp screen. */
    data object OnSwitchToSignIn : AuthFlowEvent
}

/** One-shot side effects consumed by the UI layer. */
sealed interface AuthFlowEffect {
    /** Display an error message (snackbar, toast, or inline). */
    data class ShowError(val message: String) : AuthFlowEffect
}
