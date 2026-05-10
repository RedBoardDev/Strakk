package com.strakk.android.ui.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.components.ProBadge
import com.strakk.android.ui.components.StrakkPrimaryButton
import com.strakk.android.ui.components.StrakkTextButton
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkSemanticColors
import com.strakk.android.ui.theme.StrakkTheme
import com.strakk.shared.domain.model.Feature
import com.strakk.shared.domain.model.FeatureMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureGateSheet(
    metadata: FeatureMetadata,
    onDiscoverPro: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) {
        FeatureGateSheetContent(
            metadata = metadata,
            onDiscoverPro = onDiscoverPro,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun FeatureGateSheetContent(metadata: FeatureMetadata, onDiscoverPro: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalStrakkColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        FeatureGateIcon(metadata = metadata, colors = colors)
        Spacer(Modifier.height(20.dp))
        ProBadge()
        Spacer(Modifier.height(8.dp))
        Text(
            text = resolveFeatureTitle(metadata.titleKey),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = resolveFeatureDescription(metadata.descriptionKey),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        StrakkPrimaryButton(
            text = stringResource(R.string.feature_gate_cta),
            onClick = {
                onDismiss()
                onDiscoverPro()
            },
        )
        Spacer(Modifier.height(4.dp))
        StrakkTextButton(
            text = stringResource(R.string.feature_gate_later),
            onClick = onDismiss,
            emphasized = false,
        )
    }
}

@Composable
private fun FeatureGateIcon(metadata: FeatureMetadata, colors: StrakkSemanticColors) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp).clip(CircleShape).background(colors.surface2),
        ) {
            Icon(
                imageVector = iconForAndroid(metadata.iconAndroid),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
internal fun FeatureGateSheetPreview() {
    StrakkTheme {
        FeatureGateSheet(
            metadata = FeatureMetadata(
                feature = Feature.AI_PHOTO_ANALYSIS,
                titleKey = "feature_ai_photo_title",
                descriptionKey = "feature_ai_photo_description",
                iconIos = "camera.fill",
                iconAndroid = "CameraAlt",
            ),
            onDiscoverPro = {},
            onDismiss = {},
        )
    }
}
