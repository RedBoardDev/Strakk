package com.strakk.android.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.auth.LoginRoute
import com.strakk.shared.presentation.onboarding.OnboardingFlowEffect
import com.strakk.shared.presentation.onboarding.OnboardingFlowViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingFlowRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingFlowViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OnboardingFlowEffect.NavigateToHome -> {
                    // Root reacts to auth state change automatically — no explicit nav needed.
                }
                is OnboardingFlowEffect.NavigateToLogin -> {
                    showLogin = true
                }
                is OnboardingFlowEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    if (showLogin) {
        LoginRoute(
            onNavigateToOnboarding = { showLogin = false },
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier) {
            OnboardingFlowScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onNavigateToLogin = { showLogin = true },
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
