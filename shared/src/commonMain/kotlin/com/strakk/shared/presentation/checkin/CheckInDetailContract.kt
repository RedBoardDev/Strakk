package com.strakk.shared.presentation.checkin

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInDelta

sealed interface CheckInDetailUiState {
    data object Loading : CheckInDetailUiState
    data class Ready(
        val checkIn: CheckIn,
        val delta: CheckInDelta?,
        val photoUrls: Map<String, String>,
    ) : CheckInDetailUiState
}

sealed interface CheckInDetailEvent {
    data object OnEdit : CheckInDetailEvent
    data object OnDelete : CheckInDetailEvent
    data object OnConfirmDelete : CheckInDetailEvent
}

sealed interface CheckInDetailEffect {
    data class NavigateToWizard(val checkInId: String) : CheckInDetailEffect
    data object NavigateBack : CheckInDetailEffect
    data class ShowError(val message: String) : CheckInDetailEffect
}
