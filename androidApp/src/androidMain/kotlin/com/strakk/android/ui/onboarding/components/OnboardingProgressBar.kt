package com.strakk.android.ui.onboarding.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors

@Composable
fun OnboardingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "onboarding_progress",
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        color = MaterialTheme.colorScheme.primary,
        trackColor = LocalStrakkColors.current.surface2,
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp),
    )
}
