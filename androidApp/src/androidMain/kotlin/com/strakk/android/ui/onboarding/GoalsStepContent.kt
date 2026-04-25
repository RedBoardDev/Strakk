package com.strakk.android.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
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
fun GoalsStepContent(
    proteinGoal: String,
    calorieGoal: String,
    onProteinGoalChanged: (String) -> Unit,
    onCalorieGoalChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Outlined.Restaurant,
            contentDescription = null,
            tint = LocalStrakkColors.current.textSecondary,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Set your daily goals",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "How much protein and calories do you aim for?",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalStrakkColors.current.textSecondary,
        )
        Spacer(modifier = Modifier.height(32.dp))

        GoalTextField(
            label = "Protein",
            value = proteinGoal,
            onValueChange = onProteinGoalChanged,
            placeholder = "150",
            suffix = "g / day",
            imeAction = ImeAction.Next,
        )
        Spacer(modifier = Modifier.height(16.dp))
        GoalTextField(
            label = "Calories",
            value = calorieGoal,
            onValueChange = onCalorieGoalChanged,
            placeholder = "2200",
            suffix = "kcal / day",
            imeAction = ImeAction.Done,
        )
    }
}

@Composable
private fun GoalTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suffix: String,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                color = LocalStrakkColors.current.textTertiary,
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = LocalStrakkColors.current.textTertiary,
            )
        },
        suffix = {
            Text(
                text = suffix,
                color = LocalStrakkColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = LocalStrakkColors.current.divider,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun GoalsStepContentPreview() {
    StrakkTheme {
        GoalsStepContent(
            proteinGoal = "150",
            calorieGoal = "",
            onProteinGoalChanged = {},
            onCalorieGoalChanged = {},
        )
    }
}
