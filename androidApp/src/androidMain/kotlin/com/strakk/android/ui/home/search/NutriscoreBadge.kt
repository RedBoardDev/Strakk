package com.strakk.android.ui.home.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors

private val NutriscoreColorA = Color(0xFF1F8E3D)
private val NutriscoreColorB = Color(0xFF85BB2F)
private val NutriscoreColorC = Color(0xFFF1C232)
private val NutriscoreColorD = Color(0xFFE67E22)
private val NutriscoreColorE = Color(0xFFC0392B)

@Composable
internal fun NutriscoreBadge(grade: Char, modifier: Modifier = Modifier) {
    val color = when (grade) {
        'a' -> NutriscoreColorA
        'b' -> NutriscoreColorB
        'c' -> NutriscoreColorC
        'd' -> NutriscoreColorD
        'e' -> NutriscoreColorE
        else -> LocalStrakkColors.current.textTertiary
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color,
        modifier = modifier.size(width = 22.dp, height = 22.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = grade.uppercaseChar().toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}
