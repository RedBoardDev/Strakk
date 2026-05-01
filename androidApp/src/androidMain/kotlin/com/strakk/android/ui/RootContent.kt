package com.strakk.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.strakk.android.ui.main.MainScreen
import com.strakk.android.ui.onboarding.OnboardingFlowRoute
import com.strakk.shared.presentation.auth.RootUiState

@Composable
fun RootContent(
    state: RootUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)

        when (state) {
            is RootUiState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = contentModifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is RootUiState.Unauthenticated -> {
                OnboardingFlowRoute(modifier = contentModifier)
            }
            is RootUiState.Authenticated -> {
                if (state.onboardingCompleted) {
                    MainScreen(modifier = contentModifier)
                } else {
                    OnboardingFlowRoute(modifier = contentModifier)
                }
            }
        }
    }
}
