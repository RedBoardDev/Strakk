package com.strakk.android.ui.home.draft

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.strakk.android.R
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.shared.domain.model.DraftItem

private const val TAG = "DraftItemRow"

@Composable
internal fun DraftItemRow(
    item: DraftItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        when (item) {
            is DraftItem.Resolved -> ResolvedItemRow(item = item)
            is DraftItem.PendingPhoto -> PendingPhotoRow(item = item)
            is DraftItem.PendingText -> PendingTextRow(item = item)
        }
    }
}

@Composable
internal fun ResolvedItemRow(
    item: DraftItem.Resolved,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.entry.name ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.entry.quantity?.let { qty ->
                Text(
                    text = qty,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalStrakkColors.current.textSecondary,
                )
            }
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("${item.entry.protein.toInt()}g")
                }
                withStyle(SpanStyle(color = LocalStrakkColors.current.textSecondary)) {
                    append(" · ${item.entry.calories.toInt()} kcal")
                }
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun PendingPhotoRow(
    item: DraftItem.PendingPhoto,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val bitmap = remember(item.imageBase64) {
            try {
                val bytes = Base64.decode(item.imageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                Log.w(TAG, "PendingPhotoRow: failed to decode Base64 image", e)
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.size(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.hint ?: stringResource(R.string.meal_draft_photo_no_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            PendingBadge(label = stringResource(R.string.meal_draft_pending_photo))
        }
    }
}

@Composable
internal fun PendingTextRow(
    item: DraftItem.PendingText,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            PendingBadge(label = stringResource(R.string.meal_draft_pending_text))
        }
    }
}

@Composable
internal fun PendingBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LocalStrakkColors.current.warning.copy(alpha = 0.15f),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalStrakkColors.current.warning,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
