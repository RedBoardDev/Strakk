package com.strakk.shared.presentation.hevy

import com.strakk.shared.domain.model.HevyExportResult
import com.strakk.shared.domain.model.WorkoutSession

// =============================================================================
// UiState
// =============================================================================

/**
 * State for the Hevy export flow.
 *
 * State machine:
 * ```
 * Idle
 *   → Parsing           (PDF selected, Edge Function in progress)
 *   → SessionList       (PDF parsed, user picks a session)
 *   → Exporting         (session selected, export in progress)
 *   → Done              (export succeeded)
 *
 * Parsing failure       → Idle        (ShowError effect emitted)
 * Exporting failure     → SessionList (ShowError effect emitted)
 * Missing API key       → Idle        (RequireApiKey effect emitted)
 * OnExportAnother       → SessionList (from Done)
 * OnDismiss             → Dismiss effect emitted
 * ```
 */
sealed interface HevyExportUiState {

    /** Initial state — no PDF selected yet. */
    data object Idle : HevyExportUiState

    /** A PDF has been submitted and the `parse-workout-pdf` Edge Function is running. */
    data object Parsing : HevyExportUiState

    /**
     * PDF was parsed successfully. The user selects which session to export.
     *
     * @property programName Parsed program name shown as a title.
     * @property sessions List of session names the user can pick from.
     */
    data class SessionList(
        val programName: String,
        val sessions: List<WorkoutSession>,
    ) : HevyExportUiState

    /**
     * A session is being exported to Hevy.
     *
     * @property sessionName Name of the session currently being exported.
     */
    data class Exporting(val sessionName: String) : HevyExportUiState

    /**
     * Export completed successfully.
     *
     * @property result Details about what was created or matched in Hevy.
     */
    data class Done(val result: HevyExportResult) : HevyExportUiState
}

// =============================================================================
// Events (UI → ViewModel)
// =============================================================================

/** User-initiated actions on the Hevy export screen. */
sealed interface HevyExportEvent {

    /**
     * User selected a PDF file.
     *
     * @property pdfBase64 Base64-encoded PDF content.
     */
    data class OnPdfSelected(val pdfBase64: String) : HevyExportEvent

    /**
     * User tapped a session to export.
     *
     * @property sessionIndex Index into the [HevyExportUiState.SessionList.sessions] list.
     */
    data class OnSessionSelected(val sessionIndex: Int) : HevyExportEvent

    /** User taps "Export another session" from the [HevyExportUiState.Done] state. */
    data object OnExportAnother : HevyExportEvent

    /** User dismisses the export sheet. */
    data object OnDismiss : HevyExportEvent
}

// =============================================================================
// Effects (ViewModel → UI, one-shot)
// =============================================================================

/** One-shot side effects consumed by the platform UI layer. */
sealed interface HevyExportEffect {

    /**
     * Export completed — show a success confirmation.
     *
     * @property routineTitle Title of the routine as stored in Hevy.
     */
    data class ExportSuccess(val routineTitle: String) : HevyExportEffect

    /** Display a transient error message (toast / snackbar). */
    data class ShowError(val message: String) : HevyExportEffect

    /** The Hevy API key is missing — the UI should prompt the user to enter one. */
    data object RequireApiKey : HevyExportEffect

    /** Dismiss the export screen. */
    data object Dismiss : HevyExportEffect
}
