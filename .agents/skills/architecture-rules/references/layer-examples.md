# Clean Architecture — Good vs Bad Examples

## Rule 1: Domain NEVER imports Data

### BAD — Domain depends on data layer

```kotlin
package com.strakk.shared.domain.usecase

import com.strakk.shared.data.remote.dto.SessionDto  // VIOLATION: domain imports data
import com.strakk.shared.data.repository.SessionRepositoryImpl  // VIOLATION: concrete class

class GetSessionsUseCase(
    private val repository: SessionRepositoryImpl,  // VIOLATION: depends on implementation
) {
    operator fun invoke(): Flow<List<SessionDto>> =  // VIOLATION: DTO in domain
        repository.getSessions()
}
```

### GOOD — Domain depends only on its own interfaces

```kotlin
package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.Session
import com.strakk.shared.domain.repository.SessionRepository  // Interface, defined in domain
import kotlinx.coroutines.flow.Flow

class GetSessionsUseCase(
    private val repository: SessionRepository,  // Interface only
) {
    operator fun invoke(): Flow<List<Session>> =  // Domain model only
        repository.getSessions()
}
```

---

## Rule 2: ViewModel always goes through Use Case

### BAD — ViewModel calls repository directly

```kotlin
package com.strakk.shared.presentation.session

import com.strakk.shared.domain.repository.SessionRepository

class SessionListViewModel(
    private val repository: SessionRepository,  // VIOLATION: skips use case layer
) : ViewModel() {

    val uiState = repository.getSessions()  // VIOLATION: direct repo call
        .map { SessionListUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionListUiState.Loading)
}
```

### GOOD — ViewModel delegates to use case

```kotlin
package com.strakk.shared.presentation.session

import com.strakk.shared.domain.usecase.GetSessionsUseCase

class SessionListViewModel(
    getSessionsUseCase: GetSessionsUseCase,  // Use case as dependency
) : ViewModel() {

    val uiState = getSessionsUseCase()  // Callable via operator invoke
        .map { SessionListUiState.Success(sessions = it) }
        .catch { emit(SessionListUiState.Error(message = it.message.orEmpty())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionListUiState.Loading,
        )
}
```

---

## Rule 3: Data layer classes are internal

### BAD — Data classes are public

```kotlin
package com.strakk.shared.data.repository

// VIOLATION: public — accessible from presentation layer
class SessionRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : SessionRepository {
    // ...
}

// VIOLATION: public DTO — can leak to other layers
@Serializable
data class SessionDto(
    val id: String,
    val name: String,
)
```

### GOOD — Data classes are internal

```kotlin
package com.strakk.shared.data.repository

// Correct: internal — only accessible within the data module
internal class SessionRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : SessionRepository {
    // ...
}

// Correct: internal DTO
@Serializable
internal data class SessionDto(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,
)

// Correct: internal mapper
internal fun SessionDto.toDomain(): Session = Session(
    id = SessionId(value = id),
    name = name,
)
```

---

## Rule 4: No DTOs in domain layer

### BAD — DTO leaks into domain and presentation

```kotlin
// Domain model IS the DTO — serialization concern pollutes domain
package com.strakk.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable  // VIOLATION: serialization in domain

@Serializable  // VIOLATION
data class Session(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,  // VIOLATION: snake_case JSON concern
)
```

```kotlin
// Presentation uses the DTO directly
package com.strakk.shared.presentation.session

import com.strakk.shared.data.remote.dto.SessionDto  // VIOLATION: data import

sealed interface SessionListUiState {
    data class Success(val sessions: List<SessionDto>) : SessionListUiState  // VIOLATION
}
```

### GOOD — Clean separation with mapping at the data boundary

```kotlin
// Domain model — pure Kotlin, no framework annotations
package com.strakk.shared.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class SessionId(val value: String)

data class Session(
    val id: SessionId,
    val name: String,
    val exerciseCount: Int,
)
```

```kotlin
// DTO stays in data layer — internal, with serialization
package com.strakk.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SessionDto(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("exercise_count") val exerciseCount: Int,
)
```

```kotlin
// Mapper at data boundary — converts DTO to domain
package com.strakk.shared.data.mapper

import com.strakk.shared.data.remote.dto.SessionDto
import com.strakk.shared.domain.model.Session
import com.strakk.shared.domain.model.SessionId

internal fun SessionDto.toDomain(): Session = Session(
    id = SessionId(value = id),
    name = name,
    exerciseCount = exerciseCount,
)
```

---

## Rule 5: No framework types in domain interfaces

### BAD — Framework types in domain

```kotlin
package com.strakk.shared.domain.repository

import androidx.lifecycle.LiveData          // VIOLATION: Android framework
import io.github.jan.supabase.SupabaseClient  // VIOLATION: Supabase framework

interface SessionRepository {
    fun getSessions(): LiveData<List<Session>>  // VIOLATION: LiveData is Android-only
    fun getClient(): SupabaseClient             // VIOLATION: framework leak
}
```

### GOOD — Pure Kotlin types only

```kotlin
package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.Exercise
import com.strakk.shared.domain.model.Session
import com.strakk.shared.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getSessions(): Flow<List<Session>>
    fun getSessionById(sessionId: SessionId): Flow<Session>
    suspend fun createSession(name: String, exercises: List<Exercise>): Result<Session>
    suspend fun deleteSession(sessionId: SessionId): Result<Unit>
}
```

---

## Rule 6: Presentation NEVER imports Data

### BAD — Composable reaches into data layer

```kotlin
package com.strakk.androidApp.ui.session

import com.strakk.shared.data.repository.SessionRepositoryImpl  // VIOLATION
import com.strakk.shared.data.remote.dto.SessionDto              // VIOLATION

@Composable
fun SessionScreen(repo: SessionRepositoryImpl) {  // VIOLATION: data dependency
    val sessions = repo.getSessions().collectAsState()
    // ...
}
```

### GOOD — Composable only knows about presentation and domain

```kotlin
package com.strakk.androidApp.ui.session

import com.strakk.shared.presentation.session.SessionListUiState
import com.strakk.shared.presentation.session.SessionListEvent

@Composable
fun SessionListScreen(
    uiState: SessionListUiState,
    onEvent: (SessionListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Renders state, emits events — no data layer knowledge
}
```
