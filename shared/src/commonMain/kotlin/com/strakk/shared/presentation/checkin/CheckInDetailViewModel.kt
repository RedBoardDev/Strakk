package com.strakk.shared.presentation.checkin

import androidx.lifecycle.viewModelScope
import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.domain.model.CheckInMeasurements
import com.strakk.shared.domain.usecase.DeleteCheckInUseCase
import com.strakk.shared.domain.usecase.GetCheckInDeltaUseCase
import com.strakk.shared.domain.usecase.GetCheckInPhotoUrlUseCase
import com.strakk.shared.domain.usecase.ObserveCheckInUseCase
import com.strakk.shared.presentation.common.MviViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class CheckInDetailViewModel(
    private val checkInId: String,
    private val observeCheckIn: ObserveCheckInUseCase,
    private val getCheckInDelta: GetCheckInDeltaUseCase,
    private val getCheckInPhotoUrl: GetCheckInPhotoUrlUseCase,
    private val deleteCheckIn: DeleteCheckInUseCase,
) : MviViewModel<CheckInDetailUiState, CheckInDetailEvent, CheckInDetailEffect>(CheckInDetailUiState.Loading) {

    init { observe() }

    override fun onEvent(event: CheckInDetailEvent) {
        when (event) {
            CheckInDetailEvent.OnEdit -> emit(CheckInDetailEffect.NavigateToWizard(checkInId))
            CheckInDetailEvent.OnDelete -> { /* UI shows confirmation dialog */ }
            CheckInDetailEvent.OnConfirmDelete -> launchDelete()
        }
    }

    private fun observe() {
        viewModelScope.launch {
            observeCheckIn(checkInId)
                .filterNotNull()
                .collect { checkIn ->
                    val delta = loadDelta(checkIn)
                    val photoUrls = loadPhotoUrls(checkIn)
                    setState {
                        CheckInDetailUiState.Ready(
                            checkIn = checkIn,
                            delta = delta,
                            photoUrls = photoUrls,
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

    private fun launchDelete() {
        viewModelScope.launch {
            deleteCheckIn(checkInId)
                .onSuccess { emit(CheckInDetailEffect.NavigateBack) }
                .onFailure { emit(CheckInDetailEffect.ShowError(it.message ?: "An error occurred")) }
        }
    }
}
