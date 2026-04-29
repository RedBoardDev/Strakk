package com.strakk.shared.presentation.calendar

import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.EntrySource
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry

// =============================================================================
// Detail model
// =============================================================================

/**
 * Aggregated data for a single selected calendar day.
 *
 * @property date ISO-8601 date string ("yyyy-MM-dd").
 * @property summary Aggregated nutrition and hydration totals for the day.
 * @property meals All meal entries logged on that day.
 * @property waterEntries All water entries logged on that day.
 */
data class CalendarDayDetail(
    val date: String,
    val summary: DailySummary,
    val meals: List<MealEntry>,
    val mealContainers: List<Meal>,
    val waterEntries: List<WaterEntry>,
)

// =============================================================================
// UiState
// =============================================================================

sealed interface CalendarUiState {

    /** Initial load is in progress. */
    data object Loading : CalendarUiState

    /**
     * Calendar is ready to display.
     *
     * @property year Currently displayed year.
     * @property month Currently displayed month (1–12).
     * @property activeDays Set of ISO-8601 date strings with logged data.
     * @property selectedDay The date the user tapped, or null if none selected.
     * @property dayDetail Detail data for [selectedDay], or null while loading or unselected.
     * @property isDayDetailLoading True while the day detail is being fetched.
     */
    data class Ready(
        val year: Int,
        val month: Int,
        val activeDays: Set<String>,
        val selectedDay: String? = null,
        val dayDetail: CalendarDayDetail? = null,
        val isDayDetailLoading: Boolean = false,
    ) : CalendarUiState
}

// =============================================================================
// Events (UI → ViewModel)
// =============================================================================

sealed interface CalendarEvent {

    /** User navigated to a different month. */
    data class SelectMonth(val year: Int, val month: Int) : CalendarEvent

    /** User tapped a calendar day cell. */
    data class SelectDay(val date: String) : CalendarEvent

    /** User dismissed the day detail panel/sheet. */
    data object DismissDay : CalendarEvent

    data class OnDeleteOrphanEntry(val id: String) : CalendarEvent

    data class OnDeleteMeal(val mealId: String) : CalendarEvent

    data class OnUpdateEntry(
        val id: String,
        val mealId: String?,
        val logDate: String,
        val source: EntrySource,
        val createdAt: String,
        val name: String,
        val protein: Double,
        val calories: Double,
        val fat: Double?,
        val carbs: Double?,
        val quantity: String?,
    ) : CalendarEvent

    data class OnAddWater(val date: String, val amount: Int) : CalendarEvent

    data class OnRemoveWater(val date: String, val amount: Int) : CalendarEvent
}

// =============================================================================
// Effects (ViewModel → UI, one-shot)
// =============================================================================

sealed interface CalendarEffect {

    /** Navigate to the meal entry screen, pre-scoped to [date]. */
    data class OpenMealEntryForDay(val date: String) : CalendarEffect

    /** Display a transient error message. */
    data class ShowError(val message: String) : CalendarEffect
}
