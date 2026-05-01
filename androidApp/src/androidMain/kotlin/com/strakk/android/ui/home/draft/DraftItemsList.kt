package com.strakk.android.ui.home.draft

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.presentation.meal.MealDraftUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DraftItemsList(
    state: MealDraftUiState.Editing,
    onRemoveItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            TotalsCard(state = state)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.meal_draft_items_section),
                style = MaterialTheme.typography.labelSmall,
                color = LocalStrakkColors.current.textTertiary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.draft.items.isEmpty()) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                ) {
                    Text(
                        text = stringResource(R.string.meal_draft_empty_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalStrakkColors.current.textSecondary,
                    )
                }
            }
        } else {
            items(
                items = state.draft.items,
                key = { it.id },
            ) { item ->
                val dismissState = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        onRemoveItem(item.id)
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Surface(
                            color = LocalStrakkColors.current.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 16.dp),
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.meal_draft_delete),
                                    tint = LocalStrakkColors.current.error,
                                )
                            }
                        }
                    },
                    enableDismissFromStartToEnd = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                ) {
                    DraftItemRow(
                        item = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
