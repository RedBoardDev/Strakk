package com.strakk.shared.presentation.checkin

import com.strakk.shared.domain.model.CheckInListItem

sealed interface CheckInListUiState {
    data object Loading : CheckInListUiState
    data class Ready(
        val checkIns: List<CheckInListItem>,
        val quickStats: QuickStats?,
    ) : CheckInListUiState
}

data class QuickStats(
    val lastWeight: Double?,
    val weightDelta: Double?,
    val lastAvgArm: Double?,
    val armDelta: Double?,
    val lastWaist: Double?,
    val waistDelta: Double?,
)

sealed interface CheckInListEvent {
    data object OnCreateNew : CheckInListEvent
    data class OnOpenDetail(val id: String) : CheckInListEvent
    data object OnOpenStats : CheckInListEvent
}

sealed interface CheckInListEffect {
    data object NavigateToWizard : CheckInListEffect
    data class NavigateToDetail(val id: String) : CheckInListEffect
    data object NavigateToStats : CheckInListEffect
}
