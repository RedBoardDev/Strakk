package com.strakk.shared.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request body for the `parse-workout-pdf` Supabase Edge Function. */
@Serializable
internal data class ParseWorkoutPdfRequestDto(
    @SerialName("pdf_base64") val pdfBase64: String,
)

/** Response from the `parse-workout-pdf` Supabase Edge Function. */
@Serializable
internal data class ParseWorkoutPdfResponseDto(
    @SerialName("program_name") val programName: String,
    val sessions: List<WorkoutSessionDto>,
)

/** DTO for a single training session. */
@Serializable
internal data class WorkoutSessionDto(
    val name: String,
    val sections: List<ExerciseSectionDto>,
)

/** DTO for a named exercise section within a session. */
@Serializable
internal data class ExerciseSectionDto(
    val name: String,
    val exercises: List<ProgramExerciseDto>,
)

/** DTO for a single exercise entry. */
@Serializable
internal data class ProgramExerciseDto(
    val name: String,
    val sets: Int,
    val reps: String,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("weight_per_set") val weightPerSet: List<Double?> = emptyList(),
    @SerialName("rest_seconds") val restSeconds: Int,
    val notes: String? = null,
    @SerialName("superset_group") val supersetGroup: Int? = null,
    @SerialName("exercise_type") val exerciseType: String,
    @SerialName("equipment_category") val equipmentCategory: String,
    @SerialName("muscle_group") val muscleGroup: String,
)

/** Request body for the `export-to-hevy` Supabase Edge Function. */
@Serializable
internal data class ExportToHevyRequestDto(
    val session: WorkoutSessionDto,
    // hevy_api_key removed — the server reads it from the profiles table via decrypt_hevy_api_key RPC
)

/** Response from the `export-to-hevy` Supabase Edge Function. */
@Serializable
internal data class ExportToHevyResponseDto(
    @SerialName("routine_id") val routineId: String,
    @SerialName("routine_title") val routineTitle: String,
    @SerialName("exercises_matched") val exercisesMatched: Int,
    @SerialName("exercises_created") val exercisesCreated: Int,
    @SerialName("exercises_matched_by_algo") val exercisesMatchedByAlgo: Int = 0,
    @SerialName("exercises_matched_by_ai") val exercisesMatchedByAi: Int = 0,
)
