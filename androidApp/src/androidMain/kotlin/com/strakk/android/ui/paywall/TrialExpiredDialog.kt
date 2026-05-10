package com.strakk.android.ui.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.strakk.android.R
import com.strakk.android.ui.components.StrakkPrimaryButton
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.shared.domain.model.PaywallOfferPrices
import com.strakk.shared.domain.usecase.LoadPaywallOfferPricesUseCase
import org.koin.compose.koinInject

@Composable
fun TrialExpiredDialog(onDiscoverPlans: () -> Unit, onContinueFree: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(
        onDismissRequest = onContinueFree,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(enabled = false, onClick = {}),
        ) {
            TrialExpiredCard(
                onDiscoverPlans = onDiscoverPlans,
                onContinueFree = onContinueFree,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )
        }
    }
}

@Composable
private fun TrialExpiredCard(onDiscoverPlans: () -> Unit, onContinueFree: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalStrakkColors.current
    val loadPrices: LoadPaywallOfferPricesUseCase = koinInject()
    var offerPrices by remember { mutableStateOf(PaywallOfferPrices()) }
    LaunchedEffect(Unit) {
        offerPrices = loadPrices()
    }

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface3)
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.trial_expired_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.trial_expired_body),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        TrialPriceText(offerPrices = offerPrices)
        Spacer(Modifier.height(24.dp))
        StrakkPrimaryButton(
            text = stringResource(R.string.trial_expired_cta),
            onClick = onDiscoverPlans,
        )
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onContinueFree,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(
                text = stringResource(R.string.trial_expired_free),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun TrialPriceText(offerPrices: PaywallOfferPrices) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current
    val annual = offerPrices.annualFormatted?.trim().orEmpty()
    val perMonth = offerPrices.annualPricePerMonthFormatted?.trim().orEmpty()
    val priceRaw = when {
        annual.isNotEmpty() && perMonth.isNotEmpty() ->
            stringResource(R.string.trial_expired_price_line, annual, perMonth)
        annual.isNotEmpty() -> annual
        else -> stringResource(R.string.paywall_price_placeholder)
    }
    val separatorIndex = priceRaw.indexOf('·')
    val priceAnnotated = buildAnnotatedString {
        if (separatorIndex >= 0) {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                append(priceRaw.substring(0, separatorIndex + 1))
            }
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.accentOrange)) {
                append(priceRaw.substring(separatorIndex + 1))
            }
        } else {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                append(priceRaw)
            }
        }
    }
    Text(text = priceAnnotated, style = textStyles.bodyBold)
}
