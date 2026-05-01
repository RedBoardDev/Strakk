package com.strakk.android.ui.home.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme

private const val MAX_CHARS = 300
private const val MIN_CHARS = 3
private const val WARNING_THRESHOLD = 0.8f

// =============================================================================
// Screen (callback-driven, no ViewModel — parent routes the result)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEntryScreen(
    onNavigateBack: () -> Unit,
    onSubmit: (description: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }

    val isValid = text.length in MIN_CHARS..MAX_CHARS
    val charRatio = text.length.toFloat() / MAX_CHARS

    val counterColor: Color = when {
        charRatio >= 1f -> LocalStrakkColors.current.error
        charRatio >= WARNING_THRESHOLD -> LocalStrakkColors.current.warning
        else -> LocalStrakkColors.current.textTertiary
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.text_entry_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.text_entry_back_cd))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = { if (isValid) onSubmit(text.trim()) },
                    enabled = isValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = LocalStrakkColors.current.surface2,
                        disabledContentColor = LocalStrakkColors.current.textTertiary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.text_entry_add_to_meal))
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= MAX_CHARS) text = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.text_entry_placeholder),
                        color = LocalStrakkColors.current.textTertiary,
                    )
                },
                supportingText = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = "${text.length}/$MAX_CHARS",
                            style = MaterialTheme.typography.labelSmall,
                            color = counterColor,
                        )
                    }
                },
                isError = text.isNotEmpty() && !isValid,
                singleLine = false,
                minLines = 5,
                maxLines = 10,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = LocalStrakkColors.current.divider,
                    errorBorderColor = LocalStrakkColors.current.error,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun TextEntryScreenPreview() {
    StrakkTheme {
        TextEntryScreen(
            onNavigateBack = {},
            onSubmit = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun TextEntryScreenFilledPreview() {
    StrakkTheme {
        // Simulate filled state via a wrapper
        Text(
            text = "Preview with text requires state — see runtime",
            color = LocalStrakkColors.current.textSecondary,
            modifier = Modifier.padding(16.dp),
        )
    }
}
