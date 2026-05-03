package com.strakk.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strakk.android.ui.main.MainScreen
import com.strakk.android.ui.onboarding.OnboardingFlowRoute
import com.strakk.android.ui.paywall.PaywallRoute
import com.strakk.android.ui.paywall.TrialExpiredDialog
import com.strakk.shared.presentation.auth.RootUiState
import com.strakk.shared.presentation.auth.RootViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RootContent(modifier: Modifier = Modifier, viewModel: RootViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPaywallFromTrialExpired by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)

        when (val currentState = state) {
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
                if (currentState.onboardingCompleted) {
                    MainScreen(modifier = contentModifier)
                } else {
                    OnboardingFlowRoute(modifier = contentModifier)
                }

                if (currentState.showTrialExpiredModal && !showPaywallFromTrialExpired) {
                    TrialExpiredDialog(
                        onDiscoverPlans = {
                            viewModel.dismissTrialModal()
                            showPaywallFromTrialExpired = true
                        },
                        onContinueFree = { viewModel.dismissTrialModal() },
                    )
                }

                if (showPaywallFromTrialExpired) {
                    PaywallRoute(
                        onDismiss = { showPaywallFromTrialExpired = false },
                        modifier = contentModifier,
                    )
                }
            }
        }
    }
}
