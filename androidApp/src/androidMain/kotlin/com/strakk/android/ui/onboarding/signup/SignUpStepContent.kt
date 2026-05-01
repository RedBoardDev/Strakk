package com.strakk.android.ui.onboarding.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.StrakkTheme

@Composable
fun SignUpStepContent(
    email: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val spacing = LocalStrakkSpacing.current
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_signup_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text(stringResource(R.string.onboarding_signup_email_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = colors.borderSubtle,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = colors.textSecondary,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.md))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            label = { Text(stringResource(R.string.onboarding_signup_password_label)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) {
                            stringResource(R.string.onboarding_signup_password_hide)
                        } else {
                            stringResource(R.string.onboarding_signup_password_show)
                        },
                        tint = colors.textSecondary,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = colors.borderSubtle,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = colors.textSecondary,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(spacing.xs))

        Text(
            text = stringResource(R.string.onboarding_signup_password_caption),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        Button(
            onClick = onSignUp,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.onboarding_signup_cta),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(R.string.onboarding_signup_login_link),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050918)
@Composable
private fun SignUpStepContentPreview() {
    StrakkTheme {
        SignUpStepContent(
            email = "user@example.com",
            password = "",
            isLoading = false,
            error = null,
            onEmailChanged = {},
            onPasswordChanged = {},
            onSignUp = {},
            onNavigateToLogin = {},
        )
    }
}
