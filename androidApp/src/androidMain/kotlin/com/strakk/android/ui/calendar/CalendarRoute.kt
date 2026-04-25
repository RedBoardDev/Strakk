package com.strakk.android.ui.calendar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Stateful route for the Calendar tab.
 *
 * CalendarViewModel is not yet exposed by the KMP layer. The Route is wired
 * structurally so that connecting the real VM requires only changing the
 * state source — the composition tree stays the same.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRoute(modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Stub state — will be replaced by CalendarViewModel.uiState once KMP exposes it
    var currentYear by remember { mutableStateOf(2026) }
    var currentMonth by remember { mutableStateOf(4) } // April
    var selectedDay by remember { mutableStateOf<String?>(null) }

    CalendarContent(
        year = currentYear,
        month = currentMonth,
        activeDays = emptySet(),
        onPreviousMonth = {
            if (currentMonth == 1) {
                currentMonth = 12
                currentYear -= 1
            } else {
                currentMonth -= 1
            }
        },
        onNextMonth = {
            if (currentMonth == 12) {
                currentMonth = 1
                currentYear += 1
            } else {
                currentMonth += 1
            }
        },
        onSelectDay = { date -> selectedDay = date },
        modifier = modifier,
    )

    // Day detail sheet
    if (selectedDay != null) {
        DayDetailSheet(
            date = selectedDay!!,
            sheetState = bottomSheetState,
            onDismiss = { selectedDay = null },
            onAddMealForDay = {
                // OpenMealEntryForDay effect — TodayRoute will handle the overlay
                // when CalendarContract is connected. For now dismiss the sheet.
                selectedDay = null
            },
        )
    }
}
