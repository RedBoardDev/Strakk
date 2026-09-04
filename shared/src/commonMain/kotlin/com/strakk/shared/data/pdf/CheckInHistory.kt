package com.strakk.shared.data.pdf

import com.strakk.shared.domain.model.CheckInSeriesPoint

internal fun List<CheckInSeriesPoint>.historyThroughWeek(
    inclusiveWeekLabel: String,
    limit: Int,
): List<CheckInSeriesPoint> {
    require(limit > 0) { "History limit must be positive" }
    return filter { it.weekLabel <= inclusiveWeekLabel }
        .sortedBy { it.weekLabel }
        .takeLast(limit)
}
