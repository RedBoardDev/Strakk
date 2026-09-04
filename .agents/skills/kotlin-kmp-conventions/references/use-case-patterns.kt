package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.error.DomainError
import com.strakk.shared.domain.model.Exercise
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.model.Session
import com.strakk.shared.domain.model.SessionId
import com.strakk.shared.domain.repository.ExerciseRepository
import com.strakk.shared.domain.repository.MealRepository
import com.strakk.shared.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// =============================================================================
// Pattern 1: Simple use case — single Flow return
// =============================================================================

/**
 * Observes all sessions for the current user.
 *
 * Returns a [Flow] that emits whenever the session list changes.
 */
class GetSessionsUseCase(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<List<Session>> =
        sessionRepository.getSessions()
}

// =============================================================================
// Pattern 2: Use case with Flow return and transformation
// =============================================================================

/**
 * Observes a single session with its exercises.
 *
 * Combines two repository flows into a single domain model.
 */
class ObserveSessionWithExercisesUseCase(
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    operator fun invoke(
        sessionId: SessionId,
    ): Flow<SessionWithExercises> = combine(
        sessionRepository.getSessionById(sessionId),
        exerciseRepository.getExercisesForSession(sessionId),
    ) { session, exercises ->
        SessionWithExercises(
            session = session,
            exercises = exercises,
        )
    }
}

data class SessionWithExercises(
    val session: Session,
    val exercises: List<Exercise>,
)

// =============================================================================
// Pattern 3: Use case with Result<T> error handling
// =============================================================================

/**
 * Saves a new session after validating business rules.
 *
 * Returns [Result.success] with the created [Session],
 * or [Result.failure] with a [DomainError].
 */
class CreateSessionUseCase(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        name: String,
        exercises: List<Exercise>,
    ): Result<Session> {
        if (name.isBlank()) {
            return Result.failure(DomainError.ValidationError("Session name cannot be blank"))
        }
        if (exercises.isEmpty()) {
            return Result.failure(DomainError.ValidationError("Session must have at least one exercise"))
        }

        return sessionRepository.createSession(
            name = name,
            exercises = exercises,
        )
    }
}

// =============================================================================
// Pattern 4: Use case with multiple repository dependencies
// =============================================================================

/**
 * Logs a completed meal and updates the daily nutrition summary.
 *
 * Coordinates across two repositories in a single use case.
 */
class LogMealUseCase(
    private val mealRepository: MealRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        mealEntry: MealEntry,
    ): Result<Unit> {
        val saveResult = mealRepository.saveMealEntry(mealEntry)
        if (saveResult.isFailure) {
            return saveResult
        }

        return mealRepository.refreshDailySummary(
            date = mealEntry.date,
        )
    }
}
