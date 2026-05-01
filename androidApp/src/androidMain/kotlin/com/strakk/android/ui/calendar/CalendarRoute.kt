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
import java.time.LocalDate

/**
 * Stateful route for the Calendar tab.
 *
 * Uses local state for now. CalendarViewModel exists in KMP shared and can be
 * wired here to get server-synced active days and daily detail data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRoute(modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local state — CalendarViewModel exists in KMP but is not wired here yet
    val today = remember { LocalDate.now() }
    var currentYear by remember { mutableStateOf(today.year) }
    var currentMonth by remember { mutableStateOf(today.monthValue) }
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
