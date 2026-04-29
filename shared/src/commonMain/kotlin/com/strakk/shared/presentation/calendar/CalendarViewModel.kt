package com.strakk.shared.presentation.calendar

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.usecase.DeleteMealContainerUseCase
import com.strakk.shared.domain.usecase.DeleteMealUseCase
import com.strakk.shared.domain.usecase.GetMonthlyActivityUseCase
import com.strakk.shared.domain.usecase.ObserveDailySummaryUseCase
import com.strakk.shared.domain.usecase.ObserveMealContainersForDateUseCase
import com.strakk.shared.domain.usecase.ObserveMealsForDateUseCase
import com.strakk.shared.domain.usecase.ObserveNutritionMutationsUseCase
import com.strakk.shared.domain.usecase.ObserveWaterEntriesForDateUseCase
import com.strakk.shared.domain.usecase.AddWaterUseCase
import com.strakk.shared.domain.usecase.RemoveLastWaterEntryUseCase
import com.strakk.shared.domain.usecase.UpdateMealEntryUseCase
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
    private val observeMealContainersForDate: ObserveMealContainersForDateUseCase,
    private val observeWaterEntriesForDate: ObserveWaterEntriesForDateUseCase,
    private val observeNutritionMutations: ObserveNutritionMutationsUseCase,
    private val deleteOrphanEntry: DeleteMealUseCase,
    private val deleteMealContainer: DeleteMealContainerUseCase,
    private val updateEntry: UpdateMealEntryUseCase,
    private val addWater: AddWaterUseCase,
    private val removeLastWater: RemoveLastWaterEntryUseCase,
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
            is CalendarEvent.OnDeleteOrphanEntry -> launchDeleteOrphan(event.id)
            is CalendarEvent.OnDeleteMeal -> launchDeleteMeal(event.mealId)
            is CalendarEvent.OnUpdateEntry -> launchUpdateEntry(event)
            is CalendarEvent.OnAddWater -> launchAddWater(event.date, event.amount)
            is CalendarEvent.OnRemoveWater -> launchRemoveWater(event.date, event.amount)
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
            observeMealContainersForDate(date),
            observeWaterEntriesForDate(date),
        ) { summary, meals, mealContainers, waterEntries ->
            CalendarDayDetail(
                date = date,
                summary = summary,
                meals = meals,
                mealContainers = mealContainers,
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

    private fun launchDeleteOrphan(id: String) = viewModelScope.launch {
        deleteOrphanEntry(id).onFailure { emitError(it) }
    }

    private fun launchDeleteMeal(mealId: String) = viewModelScope.launch {
        deleteMealContainer(mealId).onFailure { emitError(it) }
    }

    private fun launchUpdateEntry(event: CalendarEvent.OnUpdateEntry) = viewModelScope.launch {
        val entry = MealEntry(
            id = event.id,
            logDate = event.logDate,
            name = event.name.takeIf { it.isNotBlank() },
            protein = event.protein,
            calories = event.calories,
            fat = event.fat,
            carbs = event.carbs,
            source = event.source,
            createdAt = event.createdAt,
            mealId = event.mealId,
            quantity = event.quantity?.takeIf { it.isNotBlank() },
        )
        updateEntry(entry).onFailure { emitError(it) }
    }

    private fun launchAddWater(date: String, amount: Int) = viewModelScope.launch {
        addWater(date, amount).onFailure { emitError(it) }
    }

    private fun launchRemoveWater(date: String, amount: Int) = viewModelScope.launch {
        removeLastWater(date, amount).onFailure { emitError(it) }
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

    private fun emitError(throwable: Throwable) {
        emit(CalendarEffect.ShowError(throwable.message ?: "Une erreur est survenue."))
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
