package com.strakk.android.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.shared.presentation.auth.LoginEffect
import com.strakk.shared.presentation.auth.LoginEvent
import com.strakk.shared.presentation.auth.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateToHome -> {
                    // Root reacts to auth state change automatically.
                }
                is LoginEffect.NavigateToOnboarding -> {
                    onNavigateToOnboarding()
                }
                is LoginEffect.NavigateToSignUp -> {
                    // Navigate back to onboarding sign-up step
                    onNavigateToOnboarding()
                }
                is LoginEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Box(modifier = modifier) {
        LoginScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
