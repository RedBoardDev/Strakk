package com.strakk.shared.presentation.checkin

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.domain.model.PdfExportOptions
import com.strakk.shared.domain.model.WeeklyTrainingStats

sealed interface CheckInDetailUiState {
    data object Loading : CheckInDetailUiState
    data class Ready(
        val checkIn: CheckIn,
        val delta: CheckInDelta?,
        val photoUrls: Map<String, String>,
        val trainingStats: WeeklyTrainingStats? = null,
        val isRefreshingNutrition: Boolean = false,
        val isLoadingTraining: Boolean = false,
        val pdfExportOptions: PdfExportOptions = PdfExportOptions(),
    ) : CheckInDetailUiState
}

sealed interface CheckInDetailEvent {
    data object OnEdit : CheckInDetailEvent
    data object OnDelete : CheckInDetailEvent
    data object OnConfirmDelete : CheckInDetailEvent
    data object OnRefreshNutrition : CheckInDetailEvent
    data object OnRefreshTraining : CheckInDetailEvent
    data class OnUpdatePdfOptions(val options: PdfExportOptions) : CheckInDetailEvent
}

sealed interface CheckInDetailEffect {
    data class NavigateToWizard(val checkInId: String) : CheckInDetailEffect
    data object NavigateBack : CheckInDetailEffect
    data class ShowError(val message: String) : CheckInDetailEffect
}
