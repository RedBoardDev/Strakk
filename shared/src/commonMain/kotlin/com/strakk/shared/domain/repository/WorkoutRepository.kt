package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.HevyExportResult
import com.strakk.shared.domain.model.WorkoutProgram
import com.strakk.shared.domain.model.WorkoutSession

/**
 * Operations for parsing workout PDFs and exporting sessions to Hevy.
 *
 * Implementations live in the data layer and are `internal`.
 * Methods throw on failure — use cases wrap calls in [runSuspendCatching].
 */
interface WorkoutRepository {

    /**
     * Sends a Base64-encoded PDF to the `parse-workout-pdf` Edge Function
     * and returns a structured [WorkoutProgram].
     *
     * @param pdfBase64 Base64-encoded PDF content.
     * @return The parsed [WorkoutProgram].
     * @throws Exception on network or parsing errors.
     */
    suspend fun parseWorkoutPdf(pdfBase64: String): WorkoutProgram

    /**
     * Exports a single [WorkoutSession] to Hevy via the `export-to-hevy` Edge Function.
     *
     * The Hevy API key is read server-side from the user's profile — it is never sent
     * from the client.
     *
     * @param session The session to export.
     * @return A [HevyExportResult] describing what was created/matched in Hevy.
     * @throws Exception on network or Hevy API errors.
     */
    suspend fun exportSessionToHevy(session: WorkoutSession): HevyExportResult
}
