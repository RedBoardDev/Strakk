package com.strakk.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.auth.AuthFlowEvent
import com.strakk.shared.presentation.auth.AuthFlowUiState

@Composable
fun AuthFlowScreen(
    uiState: AuthFlowUiState,
    onEvent: (AuthFlowEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseModifier = modifier
        .fillMaxSize()
        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)

    when (uiState) {
        is AuthFlowUiState.Welcome -> {
            WelcomeContent(
                onContinueWithEmail = { onEvent(AuthFlowEvent.OnContinueWithEmail) },
                modifier = baseModifier,
            )
        }
        is AuthFlowUiState.SignIn -> {
            SignInContent(
                email = uiState.email,
                password = uiState.password,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onEmailChanged = { onEvent(AuthFlowEvent.OnEmailChanged(it)) },
                onPasswordChanged = { onEvent(AuthFlowEvent.OnPasswordChanged(it)) },
                onSignIn = { onEvent(AuthFlowEvent.OnSignIn) },
                onSwitchToSignUp = { onEvent(AuthFlowEvent.OnSwitchToSignUp) },
                modifier = baseModifier,
            )
        }
        is AuthFlowUiState.SignUp -> {
            SignUpContent(
                email = uiState.email,
                password = uiState.password,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onEmailChanged = { onEvent(AuthFlowEvent.OnEmailChanged(it)) },
                onPasswordChanged = { onEvent(AuthFlowEvent.OnPasswordChanged(it)) },
                onSignUp = { onEvent(AuthFlowEvent.OnSignUp) },
                onSwitchToSignIn = { onEvent(AuthFlowEvent.OnSwitchToSignIn) },
                modifier = baseModifier,
            )
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true)
@Composable
private fun AuthFlowScreenWelcomePreview() {
    StrakkTheme {
        AuthFlowScreen(
            uiState = AuthFlowUiState.Welcome,
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthFlowScreenSignInPreview() {
    StrakkTheme {
        AuthFlowScreen(
            uiState = AuthFlowUiState.SignIn(
                email = "user@example.com",
                password = "",
                isLoading = false,
                error = null,
            ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthFlowScreenSignUpPreview() {
    StrakkTheme {
        AuthFlowScreen(
            uiState = AuthFlowUiState.SignUp(
                email = "user@example.com",
                password = "",
                isLoading = false,
                error = null,
            ),
            onEvent = {},
        )
    }
}
