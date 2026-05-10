package com.strakk.android.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkTextStyles
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DraftItem

@Suppress("LongMethod", "FunctionSignature")
@Composable
internal fun ActionButtonsBar(
    onNewMeal: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Secondary: start a new named meal
            Surface(
                onClick = onNewMeal,
                shape = RoundedCornerShape(12.dp),
                color = colors.surface2,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.today_action_new_meal),
                        style = textStyles.bodyBold,
                        color = colors.textPrimary,
                    )
                }
            }

            // Primary: quick add (accent orange)
            Surface(
                onClick = onQuickAdd,
                shape = RoundedCornerShape(12.dp),
                color = colors.accentOrange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.today_action_quick_add),
                        style = textStyles.bodyBold,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DraftFloatingBar(
    draft: ActiveMealDraft,
    onTap: () -> Unit,
    onAdd: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStrakkColors.current
    val textStyles = LocalStrakkTextStyles.current

    val resolvedCount = draft.items.count { it is DraftItem.Resolved }
    val pendingCount = draft.items.size - resolvedCount
    val totalKcal = draft.items
        .filterIsInstance<DraftItem.Resolved>()
        .sumOf { it.entry.calories }
        .toInt()
    val isEmpty = draft.items.isEmpty()

    val noItemsText = stringResource(R.string.today_draft_no_items)
    val itemsCountText = stringResource(R.string.today_draft_items_count, resolvedCount)
    val pendingText = if (pendingCount > 0) {
        " " + stringResource(R.string.today_draft_pending_count, pendingCount)
    } else {
        ""
    }
    val kcalText = " " + stringResource(R.string.today_draft_kcal, totalKcal)
    val addLabel = stringResource(R.string.today_draft_add)
    val cancelLabel = stringResource(R.string.today_draft_cancel)
    val finishLabel = stringResource(R.string.today_draft_finish)

    val itemsLabel = if (isEmpty) noItemsText else "$itemsCountText$pendingText$kcalText"

    Surface(
        onClick = onTap,
        color = colors.surface2,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.name,
                    style = textStyles.bodyBold,
                    color = colors.textPrimary,
                )
                Text(
                    text = itemsLabel,
                    style = textStyles.caption,
                    color = colors.textSecondary,
                )
            }
            Surface(
                onClick = onAdd,
                shape = RoundedCornerShape(8.dp),
                color = colors.surface3,
            ) {
                Text(
                    text = addLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accentOrange,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Surface(
                onClick = if (isEmpty) onDiscard else onFinish,
                shape = RoundedCornerShape(8.dp),
                color = if (isEmpty) colors.surface3 else colors.accentOrange,
            ) {
                Text(
                    text = if (isEmpty) cancelLabel else finishLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isEmpty) colors.textSecondary else androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
