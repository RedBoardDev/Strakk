package com.strakk.android.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkRadius
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.presentation.today.TimelineItem

/**
 * Carte timeline — Section TIMELINE.
 * Empty state : icône circulaire + texte + 2 CTAs (Repas / Rapide).
 * État chargé : contenu timeline passé en slot, toujours suivi des 2 CTAs.
 */
@Composable
fun TimelineCard(
    timeline: List<TimelineItem>,
    onNavigateToDraft: () -> Unit,
    onNavigateToQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    timelineContent: (@Composable () -> Unit)? = null,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current
    val radius = LocalStrakkRadius.current

    val cardBrush = Brush.verticalGradient(
        colors = listOf(colors.surface1GradientTop, colors.surface1GradientBottom),
    )

    Surface(
        shape = RoundedCornerShape(radius.xxl),
        color = Color.Transparent,
        border = BorderStroke(width = 1.dp, color = colors.borderSubtle),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(spacing.xl),
        ) {
            if (timeline.isEmpty()) {
                TimelineEmptyState()
            } else {
                timelineContent?.invoke()
            }

            Spacer(Modifier.height(spacing.lg))

            TimelineCTAButtons(
                onNavigateToDraft = onNavigateToDraft,
                onNavigateToQuickAdd = onNavigateToQuickAdd,
            )
        }
    }
}

// =============================================================================
// Empty state
// =============================================================================

@Composable
private fun TimelineEmptyState(modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current
    val radius = LocalStrakkRadius.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(spacing.xl))

        // Conteneur circulaire 88dp
        Surface(
            shape = CircleShape,
            color = colors.surface3,
            border = BorderStroke(width = 1.dp, color = colors.borderFaint),
            modifier = Modifier.size(88.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Spacer(Modifier.height(spacing.md))

        Text(
            text = "Aucun item aujourd'hui",
            style = textStyles.heading1,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.xs))

        Text(
            text = "Utilisez les boutons ci-dessous pour commencer",
            style = textStyles.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(spacing.xl))
    }
}

// =============================================================================
// Boutons CTA Repas / Rapide
// =============================================================================

@Composable
private fun TimelineCTAButtons(
    onNavigateToDraft: () -> Unit,
    onNavigateToQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val spacing = LocalStrakkSpacing.current
    val radius = LocalStrakkRadius.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        modifier = modifier.fillMaxWidth(),
    ) {
        // ---- Bouton Repas (secondary) ----
        Surface(
            onClick = onNavigateToDraft,
            shape = RoundedCornerShape(radius.xl),
            color = colors.surface2,
            border = BorderStroke(width = 1.dp, color = colors.borderFaint),
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(spacing.xs))
                Text(
                    text = "Repas",
                    style = textStyles.heading2,
                    color = colors.textPrimary,
                )
            }
        }

        // ---- Bouton Rapide (primary orange) ----
        val rapideBrush = Brush.linearGradient(
            colors = listOf(colors.accentOrange, colors.accentOrangeLight),
        )
        Box(
            modifier = Modifier
                .weight(1.6f)
                .height(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(radius.xl),
                    ambientColor = colors.accentOrangeGlow,
                    spotColor = colors.accentOrangeGlow,
                ),
        ) {
            Surface(
                onClick = onNavigateToQuickAdd,
                shape = RoundedCornerShape(radius.xl),
                color = Color.Transparent,
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                ),
                modifier = Modifier.matchParentSize(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .background(rapideBrush, RoundedCornerShape(radius.xl)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(spacing.xs))
                        Text(
                            text = "Rapide",
                            style = textStyles.heading2,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun TimelineCardEmptyPreview() {
    StrakkTheme {
        TimelineCard(
            timeline = emptyList(),
            onNavigateToDraft = {},
            onNavigateToQuickAdd = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
