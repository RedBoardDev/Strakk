package com.strakk.shared.presentation.today

import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry

/**
 * A single chronological item in the Today timeline, either a committed Meal
 * container or an orphan quick-add entry.  Water is rendered separately.
 */
sealed interface TimelineItem {
    val createdAt: String

    data class MealContainer(val meal: Meal) : TimelineItem {
        override val createdAt: String get() = meal.createdAt.toString()
    }

    data class OrphanEntry(val entry: MealEntry) : TimelineItem {
        override val createdAt: String get() = entry.createdAt
    }
}

/** Today screen state. */
sealed interface TodayUiState {
    data object Loading : TodayUiState

    /**
     * @property dateLabel Human-readable date label, e.g. "Dim. 12 avril".
     * @property summary Aggregated nutrition totals for the day (EXCLUDES
     *   pending draft items — only committed entries count).
     * @property timeline Chronological mix of meal containers and orphan entries.
     * @property waterEntries All water entries for the day.
     * @property activeDraft The currently-active local draft, or null. When
     *   non-null, the UI displays the bottom floating bar.
     */
    data class Ready(
        val dateLabel: String,
        val summary: DailySummary,
        val timeline: List<TimelineItem>,
        val waterEntries: List<WaterEntry>,
        val activeDraft: ActiveMealDraft?,
    ) : TodayUiState
}

sealed interface TodayEvent {
    data class OnAddWater(val amount: Int) : TodayEvent
    /** Removes the most recent water entry of [amount] (or the last entry if none matches). */
    data class OnRemoveWater(val amount: Int) : TodayEvent
    data class OnDeleteWater(val id: String) : TodayEvent
    /** Delete an orphan quick-add entry. */
    data class OnDeleteOrphanEntry(val id: String) : TodayEvent
    /** Delete a whole meal container with all its entries. */
    data class OnDeleteMeal(val mealId: String) : TodayEvent
}

sealed interface TodayEffect {
    data class ShowError(val message: String) : TodayEffect
}
