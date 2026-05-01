package com.strakk.android.ui.home.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.strakk.android.ui.theme.LocalStrakkColors

@Composable
internal fun SectionOverline(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = LocalStrakkColors.current.textSecondary,
        letterSpacing = TextUnit(0.5f, TextUnitType.Sp),
        modifier = modifier,
    )
}
