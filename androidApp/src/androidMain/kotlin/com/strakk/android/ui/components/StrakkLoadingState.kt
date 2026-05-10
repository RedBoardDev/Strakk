package com.strakk.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme

/**
 * Centered loading indicator tinted with the primary accent.
 * Use as a full-screen or section placeholder while data is loading.
 * Per DESIGN.md §5 — skeleton or ProgressView must resolve within 2 seconds.
 */
@Composable
fun StrakkLoadingState(modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(40.dp),
    ) {
        CircularProgressIndicator(
            color = colors.accentOrange,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun StrakkLoadingStatePreview() {
    StrakkTheme {
        StrakkLoadingState()
    }
}
