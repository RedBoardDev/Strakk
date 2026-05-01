package com.strakk.android.ui.onboarding.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme

@Composable
fun WelcomeContent(
    onContinue: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xl)
            .padding(top = 120.dp, bottom = spacing.xxl),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(spacing.md))

            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textSecondary,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_welcome_cta),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = stringResource(R.string.onboarding_welcome_already_account),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun WelcomeContentPreview() {
    StrakkTheme {
        WelcomeContent(
            onContinue = {},
            onNavigateToLogin = {},
        )
    }
}
