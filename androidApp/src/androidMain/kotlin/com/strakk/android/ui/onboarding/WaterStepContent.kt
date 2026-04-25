package com.strakk.android.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme

@Composable
fun WaterStepContent(
    waterGoal: String,
    onWaterGoalChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Outlined.WaterDrop,
            contentDescription = null,
            tint = LocalStrakkColors.current.water,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Stay hydrated",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set your daily water intake goal",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalStrakkColors.current.textSecondary,
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = waterGoal,
            onValueChange = onWaterGoalChanged,
            label = {
                Text(
                    text = "Water",
                    color = LocalStrakkColors.current.textTertiary,
                )
            },
            placeholder = {
                Text(
                    text = "2500",
                    color = LocalStrakkColors.current.textTertiary,
                )
            },
            suffix = {
                Text(
                    text = "mL / day",
                    color = LocalStrakkColors.current.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedBorderColor = LocalStrakkColors.current.divider,
                focusedBorderColor = LocalStrakkColors.current.water,
                cursorColor = LocalStrakkColors.current.water,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun WaterStepContentPreview() {
    StrakkTheme {
        WaterStepContent(
            waterGoal = "2500",
            onWaterGoalChanged = {},
        )
    }
}
