package com.strakk.shared.presentation.checkin

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.domain.model.CheckInInput
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.usecase.ComputeNutritionSummaryUseCase
import com.strakk.shared.domain.usecase.DeleteCheckInUseCase
import com.strakk.shared.domain.usecase.GetCheckInDeltaUseCase
import com.strakk.shared.domain.usecase.GetCheckInPhotoUrlUseCase
import com.strakk.shared.domain.usecase.GetPdfExportPreferencesUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInUseCase
import com.strakk.shared.domain.usecase.RefreshTrainingStatsUseCase
import com.strakk.shared.domain.usecase.SavePdfExportPreferencesUseCase
import com.strakk.shared.domain.usecase.UpdateCheckInUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class CheckInDetailViewModel(
    private val checkInId: String,
    private val observeCheckIn: ObserveCheckInUseCase,
    private val getCheckInDelta: GetCheckInDeltaUseCase,
    private val getCheckInPhotoUrl: GetCheckInPhotoUrlUseCase,
    private val deleteCheckIn: DeleteCheckInUseCase,
    private val computeNutritionSummary: ComputeNutritionSummaryUseCase,
    private val updateCheckIn: UpdateCheckInUseCase,
    private val refreshTrainingStats: RefreshTrainingStatsUseCase,
    private val getPdfExportPreferences: GetPdfExportPreferencesUseCase,
    private val savePdfExportPreferences: SavePdfExportPreferencesUseCase,
) : MviViewModel<CheckInDetailUiState, CheckInDetailEvent, CheckInDetailEffect>(CheckInDetailUiState.Loading) {

    init { observe() }

    override fun onEvent(event: CheckInDetailEvent) {
        when (event) {
            CheckInDetailEvent.OnEdit -> emit(CheckInDetailEffect.NavigateToWizard(checkInId))
            CheckInDetailEvent.OnDelete -> { /* UI shows confirmation dialog */ }
            CheckInDetailEvent.OnConfirmDelete -> launchDelete()
            CheckInDetailEvent.OnRefreshNutrition -> launchRefreshNutrition()
            CheckInDetailEvent.OnRefreshTraining -> launchRefreshTraining()
            is CheckInDetailEvent.OnUpdatePdfOptions -> {
                savePdfExportPreferences(event.options)
                setState { (this as? CheckInDetailUiState.Ready)?.copy(pdfExportOptions = event.options) ?: this }
            }
        }
    }

    private fun observe() {
        viewModelScope.launch {
            observeCheckIn(checkInId)
                .filterNotNull()
                .collect { checkIn ->
                    val delta = loadDelta(checkIn)
                    val photoUrls = loadPhotoUrls(checkIn)
                    val savedOptions = getPdfExportPreferences()
                    setState {
                        CheckInDetailUiState.Ready(
                            checkIn = checkIn,
                            delta = delta,
                            photoUrls = photoUrls,
                            trainingStats = checkIn.trainingStats,
                            pdfExportOptions = savedOptions,
                        )
                    }
                }
        }
    }

    private suspend fun loadDelta(checkIn: CheckIn): CheckInDelta? {
        val measurements = CheckInMeasurements(
            weight = checkIn.weight,
            shoulders = checkIn.shoulders,
            chest = checkIn.chest,
            armLeft = checkIn.armLeft,
            armRight = checkIn.armRight,
            waist = checkIn.waist,
            hips = checkIn.hips,
            thighLeft = checkIn.thighLeft,
            thighRight = checkIn.thighRight,
        )
        return getCheckInDelta(checkIn.weekLabel, measurements).getOrNull()
    }

    private suspend fun loadPhotoUrls(checkIn: CheckIn): Map<String, String> {
        val urls = mutableMapOf<String, String>()
        for (photo in checkIn.photos) {
            getCheckInPhotoUrl(photo.storagePath)
                .onSuccess { urls[photo.id] = it }
        }
        return urls
    }

    private fun launchRefreshTraining() {
        val currentState = uiState.value as? CheckInDetailUiState.Ready ?: return
        setState {
            (this as? CheckInDetailUiState.Ready)?.copy(isLoadingTraining = true) ?: this
        }
        viewModelScope.launch {
            refreshTrainingStats(checkInId, currentState.checkIn.coveredDates).fold(
                onSuccess = { stats ->
                    setState {
                        (this as? CheckInDetailUiState.Ready)?.copy(
                            trainingStats = stats,
                            isLoadingTraining = false,
                        ) ?: this
                    }
                },
                onFailure = { err ->
                    setState {
                        (this as? CheckInDetailUiState.Ready)?.copy(isLoadingTraining = false) ?: this
                    }
                    emit(CheckInDetailEffect.ShowError(err.message ?: "Failed to load training data"))
                },
            )
        }
    }

    private fun launchDelete() {
        viewModelScope.launch {
            deleteCheckIn(checkInId)
                .onSuccess { emit(CheckInDetailEffect.NavigateBack) }
                .onFailure { emit(CheckInDetailEffect.ShowError(it.message ?: "An error occurred")) }
        }
    }

    private fun launchRefreshNutrition() {
        val currentState = uiState.value as? CheckInDetailUiState.Ready ?: return
        setState {
            (this as? CheckInDetailUiState.Ready)?.copy(isRefreshingNutrition = true) ?: this
        }
        viewModelScope.launch {
            val checkIn = currentState.checkIn
            computeNutritionSummary(
                coveredDates = checkIn.coveredDates,
                weightKg = checkIn.weight,
                feelingTags = checkIn.feelingTags,
                mentalFeeling = checkIn.mentalFeeling ?: "",
                physicalFeeling = checkIn.physicalFeeling ?: "",
            ).fold(
                onSuccess = { newSummary ->
                    val input = CheckInInput(
                        weekLabel = checkIn.weekLabel,
                        coveredDates = checkIn.coveredDates,
                        weight = checkIn.weight,
                        shoulders = checkIn.shoulders,
                        chest = checkIn.chest,
                        armLeft = checkIn.armLeft,
                        armRight = checkIn.armRight,
                        waist = checkIn.waist,
                        hips = checkIn.hips,
                        thighLeft = checkIn.thighLeft,
                        thighRight = checkIn.thighRight,
                        feelingTags = checkIn.feelingTags,
                        mentalFeeling = checkIn.mentalFeeling,
                        physicalFeeling = checkIn.physicalFeeling,
                        nutritionSummary = newSummary,
                    )
                    updateCheckIn(checkIn.id, input, emptyList(), emptyList())
                        .onFailure { err ->
                            setState {
                                (this as? CheckInDetailUiState.Ready)
                                    ?.copy(isRefreshingNutrition = false) ?: this
                            }
                            emit(CheckInDetailEffect.ShowError(err.message ?: "Failed to refresh"))
                        }
                },
                onFailure = { err ->
                    setState {
                        (this as? CheckInDetailUiState.Ready)
                            ?.copy(isRefreshingNutrition = false) ?: this
                    }
                    emit(CheckInDetailEffect.ShowError(err.message ?: "Failed to refresh"))
                },
            )
        }
    }
}
