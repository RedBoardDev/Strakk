package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.ExerciseSectionDto
import com.strakk.shared.data.dto.ExportToHevyResponseDto
import com.strakk.shared.data.dto.ParseWorkoutPdfResponseDto
import com.strakk.shared.data.dto.ProgramExerciseDto
import com.strakk.shared.data.dto.WorkoutSessionDto
import com.strakk.shared.domain.model.ExerciseSection
import com.strakk.shared.domain.model.HevyExportResult
import com.strakk.shared.domain.model.ProgramExercise
import com.strakk.shared.domain.model.WorkoutProgram
import com.strakk.shared.domain.model.WorkoutSession

// =============================================================================
// DTO → Domain
// =============================================================================

/** Maps the `parse-workout-pdf` Edge Function response to a domain [WorkoutProgram]. */
internal fun ParseWorkoutPdfResponseDto.toDomain(): WorkoutProgram = WorkoutProgram(
    programName = programName,
    sessions = sessions.map { it.toDomain() },
)

internal fun WorkoutSessionDto.toDomain(): WorkoutSession = WorkoutSession(
    name = name,
    sections = sections.map { it.toDomain() },
)

internal fun ExerciseSectionDto.toDomain(): ExerciseSection = ExerciseSection(
    name = name,
    exercises = exercises.map { it.toDomain() },
)

internal fun ProgramExerciseDto.toDomain(): ProgramExercise = ProgramExercise(
    name = name,
    sets = sets,
    reps = reps,
    weightKg = weightKg,
    weightPerSet = weightPerSet,
    restSeconds = restSeconds,
    notes = notes,
    supersetGroup = supersetGroup,
    exerciseType = exerciseType,
    equipmentCategory = equipmentCategory,
    muscleGroup = muscleGroup,
)

/** Maps the `export-to-hevy` Edge Function response to a domain [HevyExportResult]. */
internal fun ExportToHevyResponseDto.toDomain(): HevyExportResult = HevyExportResult(
    routineId = routineId,
    routineTitle = routineTitle,
    exercisesMatched = exercisesMatched,
    exercisesCreated = exercisesCreated,
    exercisesMatchedByAlgo = exercisesMatchedByAlgo,
    exercisesMatchedByAi = exercisesMatchedByAi,
)

// =============================================================================
// Domain → DTO  (needed for export request body)
// =============================================================================

internal fun WorkoutSession.toDto(): WorkoutSessionDto = WorkoutSessionDto(
    name = name,
    sections = sections.map { it.toDto() },
)

internal fun ExerciseSection.toDto(): ExerciseSectionDto = ExerciseSectionDto(
    name = name,
    exercises = exercises.map { it.toDto() },
)

internal fun ProgramExercise.toDto(): ProgramExerciseDto = ProgramExerciseDto(
    name = name,
    sets = sets,
    reps = reps,
    weightKg = weightKg,
    weightPerSet = weightPerSet,
    restSeconds = restSeconds,
    notes = notes,
    supersetGroup = supersetGroup,
    exerciseType = exerciseType,
    equipmentCategory = equipmentCategory,
    muscleGroup = muscleGroup,
)
