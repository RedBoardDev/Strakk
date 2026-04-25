package com.strakk.android.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strakk.android.ui.theme.LocalStrakkColors
import com.strakk.android.ui.theme.StrakkTheme

private val DAY_LABELS = listOf("L", "M", "M", "J", "V", "S", "D")

/**
 * Stateless calendar grid for a single month.
 *
 * @param year Four-digit year.
 * @param month 1-based month index (1 = January, 12 = December).
 * @param activeDays Set of ISO date strings ("yyyy-MM-dd") that have logged data.
 * @param onSelectDay Called with the ISO date string when a day cell is tapped.
 */
@Composable
fun CalendarContent(
    year: Int,
    month: Int,
    activeDays: Set<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Month navigation header
        MonthHeader(
            year = year,
            month = month,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Day-of-week labels
        Row(modifier = Modifier.fillMaxWidth()) {
            DAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalStrakkColors.current.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar grid
        val cells = buildCalendarCells(year, month)

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            userScrollEnabled = false,
        ) {
            items(
                items = cells,
                key = { it.key },
            ) { cell ->
                DayCell(
                    cell = cell,
                    isActive = cell.date != null && activeDays.contains(cell.date),
                    onClick = { cell.date?.let { onSelectDay(it) } },
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(
    year: Int,
    month: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "Mois précédent",
                tint = LocalStrakkColors.current.textSecondary,
            )
        }

        Text(
            text = "${monthName(month)} $year",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Mois suivant",
                tint = LocalStrakkColors.current.textSecondary,
            )
        }
    }
}

@Composable
private fun DayCell(
    cell: CalendarCell,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                if (cell.date != null) Modifier.clickable(onClick = onClick) else Modifier,
            ),
    ) {
        if (cell.dayNumber != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = cell.dayNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (cell.date != null) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        LocalStrakkColors.current.textTertiary
                    },
                )
                if (isActive) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

// =============================================================================
// Calendar cell model
// =============================================================================

private data class CalendarCell(
    val key: String,
    val dayNumber: Int?,
    val date: String?,
)

/**
 * Builds a flat list of 42 cells (6 rows × 7 cols) for the given month.
 * Empty cells have null [CalendarCell.dayNumber] / [CalendarCell.date].
 *
 * Week starts on Monday (ISO 8601).
 */
private fun buildCalendarCells(year: Int, month: Int): List<CalendarCell> {
    val cells = mutableListOf<CalendarCell>()

    // Day of week for the 1st of the month (1=Mon … 7=Sun via ISO)
    val firstDayOfWeek = java.time.LocalDate.of(year, month, 1).dayOfWeek.value // 1=Mon
    val daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth()

    // Leading empty cells
    repeat(firstDayOfWeek - 1) { i ->
        cells.add(CalendarCell(key = "empty_start_$i", dayNumber = null, date = null))
    }

    // Day cells
    for (day in 1..daysInMonth) {
        val dateStr = "%04d-%02d-%02d".format(year, month, day)
        cells.add(CalendarCell(key = dateStr, dayNumber = day, date = dateStr))
    }

    // Trailing empty cells to complete the last row
    val totalCells = cells.size
    val remainder = totalCells % 7
    if (remainder != 0) {
        repeat(7 - remainder) { i ->
            cells.add(CalendarCell(key = "empty_end_$i", dayNumber = null, date = null))
        }
    }

    return cells
}

private fun monthName(month: Int): String = when (month) {
    1 -> "Janvier"
    2 -> "Février"
    3 -> "Mars"
    4 -> "Avril"
    5 -> "Mai"
    6 -> "Juin"
    7 -> "Juillet"
    8 -> "Août"
    9 -> "Septembre"
    10 -> "Octobre"
    11 -> "Novembre"
    12 -> "Décembre"
    else -> "?"
}

// =============================================================================
// Preview
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFF151720)
@Composable
private fun CalendarContentPreview() {
    StrakkTheme {
        CalendarContent(
            year = 2026,
            month = 4,
            activeDays = setOf("2026-04-03", "2026-04-07", "2026-04-12", "2026-04-18", "2026-04-21"),
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectDay = {},
        )
    }
}
