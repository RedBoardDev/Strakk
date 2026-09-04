package com.strakk.shared.data.pdf

import com.strakk.shared.domain.model.CheckInSeriesPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CheckInHistoryTest {

    @Test
    fun `history ends at exported week and respects limit`() {
        val history = listOf(
            point("2026-W04"),
            point("2026-W01"),
            point("2026-W03"),
            point("2026-W02"),
        )

        val result = history.historyThroughWeek(
            inclusiveWeekLabel = "2026-W03",
            limit = 2,
        )

        assertEquals(listOf("2026-W02", "2026-W03"), result.map { it.weekLabel })
    }

    @Test
    fun `history rejects non-positive limit`() {
        assertFailsWith<IllegalArgumentException> {
            listOf(point("2026-W01")).historyThroughWeek("2026-W01", limit = 0)
        }
    }

    private fun point(weekLabel: String) = CheckInSeriesPoint(
        weekLabel = weekLabel,
        weight = null,
        shoulders = null,
        chest = null,
        armLeft = null,
        armRight = null,
        waist = null,
        hips = null,
        thighLeft = null,
        thighRight = null,
    )
}
