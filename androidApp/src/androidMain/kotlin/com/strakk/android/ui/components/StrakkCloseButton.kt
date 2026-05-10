package com.strakk.android.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme

/**
 * Standard close action. 44dp minimum tap target, text-secondary tint.
 * Mirrors iOS `StrakkCloseButton` with `xmark` SF Symbol.
 */
@Suppress("FunctionSignature")
@Composable
fun StrakkCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    IconButton(
        onClick = onClick,
        modifier = modifier.size(44.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.common_close),
            tint = colors.textSecondary,
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun StrakkCloseButtonPreview() {
    StrakkTheme {
        StrakkCloseButton(onClick = {})
    }
}
