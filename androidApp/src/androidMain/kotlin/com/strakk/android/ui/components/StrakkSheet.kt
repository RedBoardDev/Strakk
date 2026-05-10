package com.strakk.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.LocalStrakkRadius
import com.strakk.android.ui.theme.LocalStrakkSpacing
import com.strakk.android.ui.theme.LocalStrakkTextStyles

/**
 * Standard Strakk bottom sheet wrapper.
 *
 * Provides consistent container color, drag handle, and an optional header row
 * with a centered [title] and a leading [StrakkCloseButton]. Matches the iOS
 * `StrakkSheet` pattern defined in DESIGN.md §5 (Sheets).
 *
 * For sheets that need custom headers, pass [title] = null and include your
 * own header inside [content].
 *
 * @param onDismiss Called when the sheet is dismissed (swipe or close tap).
 * @param title Optional centered navigation title. When non-null, a header row
 *   is rendered with a leading close button.
 * @param showCloseButton Whether to show the close button in the header row.
 *   Only relevant when [title] is non-null. Defaults to true.
 * @param sheetState External sheet state; defaults to skipPartiallyExpanded.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrakkSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    showCloseButton: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable () -> Unit,
) {
    val colors = LocalStrakkColors.current
    val radius = LocalStrakkRadius.current
    val spacing = LocalStrakkSpacing.current
    val textStyles = LocalStrakkTextStyles.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = radius.lg, topEnd = radius.lg),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (title != null) {
                // Header row: leading close button + centered title + trailing spacer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md)
                        .height(48.dp),
                ) {
                    if (showCloseButton) {
                        StrakkCloseButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                    Text(
                        text = title,
                        style = textStyles.heading3,
                        color = colors.textPrimary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Spacer(Modifier.height(spacing.xs))
            }
            content()
        }
    }
}
