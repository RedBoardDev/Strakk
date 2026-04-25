package com.strakk.android.ui.onboarding

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
import com.strakk.shared.presentation.onboarding.OnboardingEffect
import com.strakk.shared.presentation.onboarding.OnboardingViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoute(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OnboardingEffect.NavigateToHome -> {
                    // Navigation is handled by the root RootViewModel reacting
                    // to the new session state — no explicit nav call needed here.
                }
                is OnboardingEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Box(modifier = modifier) {
        OnboardingScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
