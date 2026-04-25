package com.strakk.shared.presentation.calendar

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.usecase.GetMonthlyActivityUseCase
import com.strakk.shared.domain.usecase.ObserveDailySummaryUseCase
import com.strakk.shared.domain.usecase.ObserveMealsForDateUseCase
import com.strakk.shared.domain.usecase.ObserveNutritionMutationsUseCase
import com.strakk.shared.domain.usecase.ObserveWaterEntriesForDateUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Manages the calendar screen.
 *
 * On initialisation, loads the active days for the current month. Month navigation
 * reloads active days. Day selection loads full detail (summary, meals, water) in parallel.
 */
class CalendarViewModel(
    private val getMonthlyActivity: GetMonthlyActivityUseCase,
    private val observeDailySummary: ObserveDailySummaryUseCase,
    private val observeMealsForDate: ObserveMealsForDateUseCase,
    private val observeWaterEntriesForDate: ObserveWaterEntriesForDateUseCase,
    private val observeNutritionMutations: ObserveNutritionMutationsUseCase,
    private val clock: ClockProvider,
) : MviViewModel<CalendarUiState, CalendarEvent, CalendarEffect>(CalendarUiState.Loading) {

    private var dayDetailJob: Job? = null

    init {
        val today = clock.today()
        loadMonth(year = today.year, month = today.monthNumber)
        observeMutations()
    }

    override fun onEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.SelectMonth -> loadMonth(year = event.year, month = event.month)
            is CalendarEvent.SelectDay -> handleSelectDay(event.date)
            is CalendarEvent.DismissDay -> {
                dayDetailJob?.cancel()
                dayDetailJob = null
                setState {
                    (this as? CalendarUiState.Ready)?.copy(
                        selectedDay = null,
                        dayDetail = null,
                    ) ?: this
                }
            }
        }
    }

    private fun loadMonth(year: Int, month: Int) {
        setState { CalendarUiState.Loading }

        val monthStart = monthStartString(year, month)
        val monthEnd = monthEndString(year, month)

        viewModelScope.launch {
            getMonthlyActivity(monthStart = monthStart, monthEnd = monthEnd)
                .onSuccess { activeDates ->
                    setState {
                        CalendarUiState.Ready(
                            year = year,
                            month = month,
                            activeDays = activeDates.toSet(),
                        )
                    }
                }
                .onFailure { error ->
                    // Still show the calendar — just without activity markers
                    setState {
                        CalendarUiState.Ready(
                            year = year,
                            month = month,
                            activeDays = emptySet(),
                        )
                    }
                    emit(CalendarEffect.ShowError(error.message ?: "Failed to load calendar"))
                }
        }
    }

    private fun handleSelectDay(date: String) {
        setState {
            (this as? CalendarUiState.Ready)?.copy(
                selectedDay = date,
                dayDetail = null,
                isDayDetailLoading = true,
            ) ?: this
        }

        dayDetailJob?.cancel()
        dayDetailJob = combine(
            observeDailySummary(date),
            observeMealsForDate(date),
            observeWaterEntriesForDate(date),
        ) { summary, meals, waterEntries ->
            CalendarDayDetail(
                date = date,
                summary = summary,
                meals = meals,
                waterEntries = waterEntries,
            )
        }.onEach { detail ->
            setState {
                (this as? CalendarUiState.Ready)?.copy(
                    dayDetail = detail,
                    isDayDetailLoading = false,
                ) ?: this
            }
        }.launchIn(viewModelScope)
    }

    private fun observeMutations() {
        observeNutritionMutations().onEach {
            val current = uiState.value as? CalendarUiState.Ready ?: return@onEach
            reloadActiveDays(current.year, current.month)
        }.launchIn(viewModelScope)
    }

    private fun reloadActiveDays(year: Int, month: Int) {
        val monthStart = monthStartString(year, month)
        val monthEnd = monthEndString(year, month)
        viewModelScope.launch {
            getMonthlyActivity(monthStart = monthStart, monthEnd = monthEnd)
                .onSuccess { activeDates ->
                    setState {
                        (this as? CalendarUiState.Ready)?.copy(
                            activeDays = activeDates.toSet(),
                        ) ?: this
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Date helpers (kotlinx.datetime — no java.time)
    // -------------------------------------------------------------------------

    private fun monthStartString(year: Int, month: Int): String =
        LocalDate(year, month, 1).toString()

    private fun monthEndString(year: Int, month: Int): String {
        val firstOfNextMonth = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH)
        return firstOfNextMonth.minus(1, DateTimeUnit.DAY).toString()
    }

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0)
}
