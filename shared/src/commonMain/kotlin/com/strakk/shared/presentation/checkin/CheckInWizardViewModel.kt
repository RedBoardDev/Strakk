package com.strakk.shared.presentation.checkin

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.common.ClockProvider
import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.domain.model.CheckInInput
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.usecase.ComputeNutritionSummaryUseCase
import com.strakk.shared.domain.usecase.CreateCheckInUseCase
import com.strakk.shared.domain.usecase.GetCheckInDeltaUseCase
import com.strakk.shared.domain.usecase.GetCheckInPhotoUrlUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInsUseCase
import com.strakk.shared.domain.usecase.UpdateCheckInUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private const val MAX_FEELING_TEXT_LENGTH = 1_000

class CheckInWizardViewModel(
    private val checkInId: String?,
    private val observeCheckIn: ObserveCheckInUseCase,
    private val observeCheckIns: ObserveCheckInsUseCase,
    private val createCheckIn: CreateCheckInUseCase,
    private val updateCheckIn: UpdateCheckInUseCase,
    private val computeNutritionSummary: ComputeNutritionSummaryUseCase,
    private val getCheckInDelta: GetCheckInDeltaUseCase,
    private val getCheckInPhotoUrl: GetCheckInPhotoUrlUseCase,
    private val clock: ClockProvider,
) : MviViewModel<CheckInWizardUiState, CheckInWizardEvent, CheckInWizardEffect>(CheckInWizardUiState.Loading) {

    private var nutritionJob: Job? = null
    // Track photos removed during edit: (photoId, storagePath)
    private val deletedPhotos = mutableListOf<Pair<String, String>>()

    init {
        if (checkInId != null) {
            loadForEdit(checkInId)
        } else {
            initCreate()
        }
    }

    override fun onEvent(event: CheckInWizardEvent) {
        val state = uiState.value as? CheckInWizardUiState.Ready ?: return
        when (event) {
            CheckInWizardEvent.OnNext -> goNext(state)
            CheckInWizardEvent.OnBack -> goBack(state)
            CheckInWizardEvent.OnCancel -> emit(CheckInWizardEffect.NavigateBack)

            is CheckInWizardEvent.OnSelectWeek -> selectWeek(state, event.weekLabel)
            is CheckInWizardEvent.OnToggleDate -> toggleDate(state, event.date)

            is CheckInWizardEvent.OnWeightChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(weight = event.value) }
            is CheckInWizardEvent.OnShouldersChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(shoulders = event.value) }
            is CheckInWizardEvent.OnChestChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(chest = event.value) }
            is CheckInWizardEvent.OnArmLeftChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(armLeft = event.value) }
            is CheckInWizardEvent.OnArmRightChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(armRight = event.value) }
            is CheckInWizardEvent.OnWaistChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(waist = event.value) }
            is CheckInWizardEvent.OnHipsChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(hips = event.value) }
            is CheckInWizardEvent.OnThighLeftChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(thighLeft = event.value) }
            is CheckInWizardEvent.OnThighRightChanged ->
                setState { (this as CheckInWizardUiState.Ready).copy(thighRight = event.value) }

            is CheckInWizardEvent.OnToggleTag -> toggleTag(state, event.slug)
            is CheckInWizardEvent.OnMentalFeelingChanged ->
                setState {
                    (this as CheckInWizardUiState.Ready).copy(
                        mentalFeeling = event.text.take(MAX_FEELING_TEXT_LENGTH),
                    )
                }
            is CheckInWizardEvent.OnPhysicalFeelingChanged ->
                setState {
                    (this as CheckInWizardUiState.Ready).copy(
                        physicalFeeling = event.text.take(MAX_FEELING_TEXT_LENGTH),
                    )
                }

            is CheckInWizardEvent.OnAddPhoto -> addPhoto(state, event.imageData)
            is CheckInWizardEvent.OnRemovePhoto -> removePhoto(state, event.photoId)

            CheckInWizardEvent.OnSave -> save(state)
        }
    }

    // -------------------------------------------------------------------------
    // Init
    // -------------------------------------------------------------------------

    private fun initCreate() {
        val today = clock.today()
        val weeks = generateAvailableWeeks(today)
        val currentWeek = weeks.first()
        val weekDays = generateWeekDays(currentWeek, today)

        setState {
            CheckInWizardUiState.Ready(
                isEditMode = false,
                currentStep = WizardStep.Dates,
                weekLabel = currentWeek.weekLabel,
                availableWeeks = weeks,
                coveredDates = weekDays.filter { it.selected }.map { it.date }.toSet(),
                weekDays = weekDays,
                existingCheckInId = null,
                weight = "",
                shoulders = "",
                chest = "",
                armLeft = "",
                armRight = "",
                waist = "",
                hips = "",
                thighLeft = "",
                thighRight = "",
                delta = null,
                selectedTags = emptySet(),
                mentalFeeling = "",
                physicalFeeling = "",
                photos = emptyList(),
                nutritionSummary = null,
                nutritionLoading = false,
                saving = false,
            )
        }

        checkExistingForWeek(currentWeek.weekLabel)
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            val checkIn = observeCheckIn(id).firstOrNull() ?: run {
                emit(CheckInWizardEffect.ShowError("Check-in not found."))
                emit(CheckInWizardEffect.NavigateBack)
                return@launch
            }

            val today = clock.today()
            val weeks = generateAvailableWeeks(today)
            val matchingWeek = weeks.find { it.weekLabel == checkIn.weekLabel } ?: weeks.first()
            val weekDays = generateWeekDays(matchingWeek, today).map { day ->
                day.copy(selected = checkIn.coveredDates.contains(day.date))
            }

            val remotePhotos = checkIn.photos.map { photo ->
                WizardPhoto.Remote(
                    id = photo.id,
                    storagePath = photo.storagePath,
                    signedUrl = getCheckInPhotoUrl(photo.storagePath).getOrDefault(""),
                )
            }

            val delta = loadDeltaForWeek(checkIn.weekLabel)

            setState {
                CheckInWizardUiState.Ready(
                    isEditMode = true,
                    currentStep = WizardStep.Dates,
                    weekLabel = checkIn.weekLabel,
                    availableWeeks = weeks,
                    coveredDates = checkIn.coveredDates.toSet(),
                    weekDays = weekDays,
                    existingCheckInId = null, // In edit mode, this IS the check-in being edited
                    weight = checkIn.weight?.toString() ?: "",
                    shoulders = checkIn.shoulders?.toString() ?: "",
                    chest = checkIn.chest?.toString() ?: "",
                    armLeft = checkIn.armLeft?.toString() ?: "",
                    armRight = checkIn.armRight?.toString() ?: "",
                    waist = checkIn.waist?.toString() ?: "",
                    hips = checkIn.hips?.toString() ?: "",
                    thighLeft = checkIn.thighLeft?.toString() ?: "",
                    thighRight = checkIn.thighRight?.toString() ?: "",
                    delta = delta,
                    selectedTags = checkIn.feelingTags.toSet(),
                    mentalFeeling = checkIn.mentalFeeling ?: "",
                    physicalFeeling = checkIn.physicalFeeling ?: "",
                    photos = remotePhotos,
                    nutritionSummary = checkIn.nutritionSummary,
                    nutritionLoading = false,
                    saving = false,
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    private val stepOrder = WizardStep.entries

    private fun goNext(state: CheckInWizardUiState.Ready) {
        val currentIndex = stepOrder.indexOf(state.currentStep)
        if (currentIndex < stepOrder.lastIndex) {
            val nextStep = stepOrder[currentIndex + 1]
            setState { (this as CheckInWizardUiState.Ready).copy(currentStep = nextStep) }
            if (nextStep == WizardStep.Measurements) loadDeltaAsync(state.weekLabel)
            if (nextStep == WizardStep.Summary) loadNutritionSummary(state.coveredDates.toList(), state)
        }
    }

    private fun goBack(state: CheckInWizardUiState.Ready) {
        val currentIndex = stepOrder.indexOf(state.currentStep)
        if (currentIndex > 0) {
            setState { (this as CheckInWizardUiState.Ready).copy(currentStep = stepOrder[currentIndex - 1]) }
        } else {
            emit(CheckInWizardEffect.NavigateBack)
        }
    }

    // -------------------------------------------------------------------------
    // Step 1 — Dates
    // -------------------------------------------------------------------------

    private fun selectWeek(state: CheckInWizardUiState.Ready, weekLabel: String) {
        if (state.isEditMode) return // Week is locked in edit mode
        val week = state.availableWeeks.find { it.weekLabel == weekLabel } ?: return
        val today = clock.today()
        val weekDays = generateWeekDays(week, today)

        setState {
            (this as CheckInWizardUiState.Ready).copy(
                weekLabel = weekLabel,
                weekDays = weekDays,
                coveredDates = weekDays.filter { it.selected }.map { it.date }.toSet(),
                existingCheckInId = null,
            )
        }
        checkExistingForWeek(weekLabel)
    }

    private fun toggleDate(state: CheckInWizardUiState.Ready, date: String) {
        val newDates = if (state.coveredDates.contains(date)) {
            state.coveredDates - date
        } else {
            state.coveredDates + date
        }
        val newWeekDays = state.weekDays.map { it.copy(selected = newDates.contains(it.date)) }
        setState {
            (this as CheckInWizardUiState.Ready).copy(
                coveredDates = newDates,
                weekDays = newWeekDays,
            )
        }
    }

    private fun checkExistingForWeek(weekLabel: String) {
        viewModelScope.launch {
            val existing = observeCheckIns().firstOrNull()
                ?.items
                ?.find { it.weekLabel == weekLabel }
            setState {
                (this as? CheckInWizardUiState.Ready)?.copy(existingCheckInId = existing?.id) ?: this
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 2 — Measurements delta
    // -------------------------------------------------------------------------

    private fun loadDeltaAsync(weekLabel: String) {
        viewModelScope.launch {
            val delta = loadDeltaForWeek(weekLabel)
            setState { (this as? CheckInWizardUiState.Ready)?.copy(delta = delta) ?: this }
        }
    }

    private suspend fun loadDeltaForWeek(weekLabel: String): CheckInDelta? {
        val state = uiState.value as? CheckInWizardUiState.Ready ?: return null
        val current = CheckInMeasurements(
            weight = state.weight.parseDouble(),
            shoulders = state.shoulders.parseDouble(),
            chest = state.chest.parseDouble(),
            armLeft = state.armLeft.parseDouble(),
            armRight = state.armRight.parseDouble(),
            waist = state.waist.parseDouble(),
            hips = state.hips.parseDouble(),
            thighLeft = state.thighLeft.parseDouble(),
            thighRight = state.thighRight.parseDouble(),
        )
        return getCheckInDelta(weekLabel, current).getOrNull()
    }

    // -------------------------------------------------------------------------
    // Step 3 — Tags
    // -------------------------------------------------------------------------

    private fun toggleTag(state: CheckInWizardUiState.Ready, slug: String) {
        val newTags = if (state.selectedTags.contains(slug)) {
            state.selectedTags - slug
        } else {
            state.selectedTags + slug
        }
        setState { (this as CheckInWizardUiState.Ready).copy(selectedTags = newTags) }
    }

    // -------------------------------------------------------------------------
    // Step 4 — Photos
    // -------------------------------------------------------------------------

    private fun addPhoto(state: CheckInWizardUiState.Ready, imageData: ByteArray) {
        if (state.photos.size >= 3) return
        val photo = WizardPhoto.Local(
            id = "local-${state.photos.size}-${clock.now().toEpochMilliseconds()}",
            imageData = imageData,
        )
        setState { (this as CheckInWizardUiState.Ready).copy(photos = photos + photo) }
    }

    private fun removePhoto(state: CheckInWizardUiState.Ready, photoId: String) {
        val photo = state.photos.find { it.id == photoId } ?: return
        if (photo is WizardPhoto.Remote) {
            deletedPhotos.add(photo.id to photo.storagePath)
        }
        setState { (this as CheckInWizardUiState.Ready).copy(photos = photos.filterNot { it.id == photoId }) }
    }

    // -------------------------------------------------------------------------
    // Step 5 — Nutrition summary
    // -------------------------------------------------------------------------

    private fun loadNutritionSummary(dates: List<String>, state: CheckInWizardUiState.Ready) {
        nutritionJob?.cancel()
        setState { (this as? CheckInWizardUiState.Ready)?.copy(nutritionLoading = true) ?: this }

        nutritionJob = viewModelScope.launch {
            computeNutritionSummary(
                coveredDates = dates,
                weightKg = state.weight.parseDouble(),
                feelingTags = state.selectedTags.toList(),
                mentalFeeling = state.mentalFeeling,
                physicalFeeling = state.physicalFeeling,
            )
                .onSuccess { summary ->
                    setState {
                        (this as? CheckInWizardUiState.Ready)
                            ?.copy(nutritionSummary = summary, nutritionLoading = false)
                            ?: this
                    }
                }
                .onFailure {
                    setState { (this as? CheckInWizardUiState.Ready)?.copy(nutritionLoading = false) ?: this }
                    emit(CheckInWizardEffect.ShowError(it.message ?: "An error occurred"))
                }
        }
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    private fun save(state: CheckInWizardUiState.Ready) {
        if (state.saving) return
        setState { (this as CheckInWizardUiState.Ready).copy(saving = true) }

        viewModelScope.launch {
            val input = buildInput(state)
            val localPhotos = state.photos.filterIsInstance<WizardPhoto.Local>().map { it.imageData }

            val result = if (state.isEditMode && checkInId != null) {
                updateCheckIn(checkInId, input, localPhotos, deletedPhotos.toList())
            } else {
                createCheckIn(input, localPhotos)
            }

            result
                .onSuccess { checkIn ->
                    if (state.isEditMode) {
                        emit(CheckInWizardEffect.NavigateToDetail(checkIn.id))
                    } else {
                        emit(CheckInWizardEffect.NavigateBack)
                    }
                }
                .onFailure {
                    setState { (this as? CheckInWizardUiState.Ready)?.copy(saving = false) ?: this }
                    emit(CheckInWizardEffect.ShowError(it.message ?: "An error occurred"))
                }
        }
    }

    private fun buildInput(state: CheckInWizardUiState.Ready): CheckInInput = CheckInInput(
        weekLabel = state.weekLabel,
        coveredDates = state.coveredDates.sorted().toList(),
        weight = state.weight.parseDouble(),
        shoulders = state.shoulders.parseDouble(),
        chest = state.chest.parseDouble(),
        armLeft = state.armLeft.parseDouble(),
        armRight = state.armRight.parseDouble(),
        waist = state.waist.parseDouble(),
        hips = state.hips.parseDouble(),
        thighLeft = state.thighLeft.parseDouble(),
        thighRight = state.thighRight.parseDouble(),
        feelingTags = state.selectedTags.toList(),
        mentalFeeling = state.mentalFeeling.takeIf { it.isNotBlank() },
        physicalFeeling = state.physicalFeeling.takeIf { it.isNotBlank() },
        nutritionSummary = state.nutritionSummary,
    )

    // -------------------------------------------------------------------------
    // Week generation helpers
    // -------------------------------------------------------------------------

    private fun generateAvailableWeeks(today: LocalDate): List<WeekOption> {
        val monday = today.previousOrSame(DayOfWeek.MONDAY)
        return (0..4).map { weeksBack ->
            val weekStart = monday.minus(weeksBack * 7, DateTimeUnit.DAY)
            val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
            val weekNumber = isoWeekNumber(weekStart)
            val year = isoWeekYear(weekStart)
            WeekOption(
                weekLabel = "$year-W${weekNumber.toString().padStart(2, '0')}",
                displayLabel = "Semaine $weekNumber",
                startDate = weekStart.toString(),
                endDate = weekEnd.toString(),
            )
        }
    }

    private fun generateWeekDays(week: WeekOption, today: LocalDate): List<DayOption> {
        val start = LocalDate.parse(week.startDate)
        return (0..6).map { offset ->
            val date = start.plus(offset, DateTimeUnit.DAY)
            val dayName = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            DayOption(
                date = date.toString(),
                displayLabel = "$dayName ${date.dayOfMonth}",
                selected = date <= today,
            )
        }
    }

    private fun LocalDate.previousOrSame(target: DayOfWeek): LocalDate {
        val diff = (this.dayOfWeek.ordinal - target.ordinal + 7) % 7
        return this.minus(diff, DateTimeUnit.DAY)
    }

    private fun isoWeekNumber(date: LocalDate): Int {
        val thursday = date.plus((DayOfWeek.THURSDAY.ordinal - date.dayOfWeek.ordinal + 7) % 7, DateTimeUnit.DAY)
        val jan1 = LocalDate(thursday.year, 1, 1)
        val jan1Thursday = jan1.plus((DayOfWeek.THURSDAY.ordinal - jan1.dayOfWeek.ordinal + 7) % 7, DateTimeUnit.DAY)
        return ((thursday.toEpochDays() - jan1Thursday.toEpochDays()) / 7) + 1
    }

    private fun isoWeekYear(date: LocalDate): Int {
        val thursday = date.plus((DayOfWeek.THURSDAY.ordinal - date.dayOfWeek.ordinal + 7) % 7, DateTimeUnit.DAY)
        return thursday.year
    }

    /** Normalises comma to period before parsing — handles French decimal input. */
    private fun String.parseDouble(): Double? = this.replace(",", ".").toDoubleOrNull()
}
