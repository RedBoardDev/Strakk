package com.strakk.android.ui.onboarding.prooffer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.android.ui.theme.StrakkTheme

@Composable
fun ProOfferContent(onStartFreeTrial: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ProOfferHeader()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onStartFreeTrial,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_pro_cta),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_pro_footer),
                style = textStyles.caption,
                color = colors.textTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProOfferHeader() {
    val colors = LocalStrakkColors.current
    val headlineRaw = stringResource(R.string.onboarding_pro_headline)
    val splitIndex = headlineRaw.indexOf("7")
    val headlineAnnotated = buildAnnotatedString {
        if (splitIndex >= 0) {
            append(headlineRaw.substring(0, splitIndex))
            withStyle(SpanStyle(color = colors.accentOrange)) {
                append(headlineRaw.substring(splitIndex))
            }
        } else {
            append(headlineRaw)
        }
    }
    Column {
        Text(
            text = stringResource(R.string.onboarding_pro_before),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = headlineAnnotated,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(28.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface1)
                .padding(16.dp),
        ) {
            ProFeatureHighlightRow(
                icon = Icons.Outlined.CameraAlt,
                title = "Photo intelligente",
                description = "Prends une photo, l'IA calcule tes macros.",
            )
            Spacer(Modifier.height(16.dp))
            ProFeatureHighlightRow(
                icon = Icons.Outlined.TextFields,
                title = "Texte intelligent",
                description = "Décris ton repas, l'IA fait le reste.",
            )
            Spacer(Modifier.height(16.dp))
            ProFeatureHighlightRow(
                icon = Icons.Outlined.BarChart,
                title = "Bilan hebdo IA",
                description = "Un résumé personnalisé chaque semaine.",
            )
        }
    }
}

@Composable
private fun ProFeatureHighlightRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = LocalStrakkTextStyles.current.bodyBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = LocalStrakkTextStyles.current.caption,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
internal fun ProOfferContentPreview() {
    StrakkTheme {
        ProOfferContent(
            onStartFreeTrial = {},
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        )
    }
}
