package com.strakk.shared.presentation.today

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.model.DailySummary
import com.strakk.shared.domain.model.Meal
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.WaterEntry
import com.strakk.shared.domain.usecase.AddWaterUseCase
import com.strakk.shared.domain.usecase.DeleteMealContainerUseCase
import com.strakk.shared.domain.usecase.DeleteMealUseCase
import com.strakk.shared.domain.usecase.DeleteWaterUseCase
import com.strakk.shared.domain.usecase.ObserveActiveMealDraftUseCase
import com.strakk.shared.domain.usecase.ObserveDailySummaryUseCase
import com.strakk.shared.domain.usecase.ObserveMealContainersForDateUseCase
import com.strakk.shared.domain.usecase.ObserveMealsForDateUseCase
import com.strakk.shared.domain.usecase.ObserveWaterEntriesForDateUseCase
import com.strakk.shared.domain.usecase.RemoveLastWaterEntryUseCase
import com.strakk.shared.domain.usecase.UpdateMealEntryUseCase
import com.strakk.shared.presentation.common.MviViewModel
import com.strakk.shared.presentation.common.formatDateLabel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Manages the Today screen: daily summary, chronological timeline
 * (meals + orphan quick-adds), water log, and the active Draft banner.
 *
 * Observes reactive Flows from the repository cache — state updates
 * automatically after any mutation without requiring an explicit reload.
 */
class TodayViewModel(
    private val observeDailySummary: ObserveDailySummaryUseCase,
    private val observeMeals: ObserveMealsForDateUseCase,
    private val observeMealContainers: ObserveMealContainersForDateUseCase,
    private val observeWaterEntries: ObserveWaterEntriesForDateUseCase,
    private val observeActiveDraft: ObserveActiveMealDraftUseCase,
    private val addWater: AddWaterUseCase,
    private val deleteWater: DeleteWaterUseCase,
    private val removeLastWaterEntry: RemoveLastWaterEntryUseCase,
    private val deleteOrphanEntry: DeleteMealUseCase,
    private val deleteMealContainer: DeleteMealContainerUseCase,
    private val updateEntry: UpdateMealEntryUseCase,
    private val clock: ClockProvider,
) : MviViewModel<TodayUiState, TodayEvent, TodayEffect>(TodayUiState.Loading) {

    init {
        observeToday()
    }

    override fun onEvent(event: TodayEvent) {
        when (event) {
            is TodayEvent.OnAddWater -> launchAddWater(event.amount)
            is TodayEvent.OnRemoveWater -> launchRemoveWater(event.amount)
            is TodayEvent.OnDeleteWater -> launchDeleteWater(event.id)
            is TodayEvent.OnDeleteOrphanEntry -> launchDeleteOrphan(event.id)
            is TodayEvent.OnDeleteMeal -> launchDeleteMeal(event.mealId)
            is TodayEvent.OnUpdateEntry -> launchUpdateEntry(event)
        }
    }

    private fun observeToday() {
        val today = clock.today()
        val dateString = today.toString()
        val dateLabel = formatDateLabel(today)

        viewModelScope.launch {
            combine(
                observeDailySummary(dateString),
                observeMeals(dateString),
                observeMealContainers(dateString),
                observeWaterEntries(dateString),
                observeActiveDraft(),
            ) { summary, orphans, meals, water, draft ->
                buildReadyState(dateLabel, summary, orphans, meals, water, draft)
            }.collect { state -> setState { state } }
        }
    }

    private fun buildReadyState(
        dateLabel: String,
        summary: DailySummary,
        orphans: List<MealEntry>,
        meals: List<Meal>,
        water: List<WaterEntry>,
        draft: com.strakk.shared.domain.model.ActiveMealDraft?,
    ): TodayUiState.Ready {
        val timeline = buildList<TimelineItem> {
            meals.forEach { add(TimelineItem.MealContainer(it)) }
            orphans.forEach { add(TimelineItem.OrphanEntry(it)) }
        }.sortedBy { it.createdAt }

        return TodayUiState.Ready(
            dateLabel = dateLabel,
            summary = summary,
            timeline = timeline,
            waterEntries = water,
            activeDraft = draft,
        )
    }

    private fun launchAddWater(amount: Int) = viewModelScope.launch {
        addWater(clock.today().toString(), amount).onFailure { emitError(it) }
    }

    private fun launchRemoveWater(amount: Int) = viewModelScope.launch {
        removeLastWaterEntry(clock.today().toString(), amount).onFailure { emitError(it) }
    }

    private fun launchDeleteWater(id: String) = viewModelScope.launch {
        deleteWater(id).onFailure { emitError(it) }
    }

    private fun launchDeleteOrphan(id: String) = viewModelScope.launch {
        deleteOrphanEntry(id).onFailure { emitError(it) }
    }

    private fun launchDeleteMeal(mealId: String) = viewModelScope.launch {
        deleteMealContainer(mealId).onFailure { emitError(it) }
    }

    private fun launchUpdateEntry(event: TodayEvent.OnUpdateEntry) = viewModelScope.launch {
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

    private fun emitError(throwable: Throwable) {
        emit(TodayEffect.ShowError(throwable.message ?: "An error occurred"))
    }
}
