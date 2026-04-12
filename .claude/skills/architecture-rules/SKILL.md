---
description: "Clean Architecture dependency rules and layer boundaries"
paths:
  - "shared/**/*.kt"
---

# Clean Architecture Rules for Strakk

## Dependency Rule

```
                    +-----------+
                    |  domain/  |  <-- DEPENDS ON NOTHING
                    +-----------+
                    ^           ^
                    |           |
              +-----+--+  +----+------+
              |  data/  |  | present. |
              +---------+  +----------+
              ^                      ^
              |                      |
    +---------+------+    +----------+--------+
    |   androidApp/  |    |      iosApp/      |
    +----------------+    +-------------------+
```

## Layer Import Rules

### domain/ CAN import:
- `kotlin.*`, `kotlinx.coroutines.*`, `kotlinx.datetime.*`
- Nothing else

### domain/ CANNOT import:
- `*.data.*` (data layer)
- `*.presentation.*` (presentation layer)
- `io.ktor.*` (networking)
- `io.github.jan.supabase.*` (Supabase)
- `kotlinx.serialization.*` (serialization is a data concern)
- `android.*`, `androidx.*` (framework)
- `platform.Foundation.*` (iOS framework)
- `org.koin.*` (DI framework)

### data/ CAN import:
- `*.domain.*` (domain layer)
- `io.ktor.*`, `io.github.jan.supabase.*` (networking)
- `kotlinx.serialization.*` (for DTOs)
- `org.koin.*` (for module declarations)

### data/ CANNOT import:
- `*.presentation.*`
- `android.*` (in commonMain)
- UI frameworks

### presentation/ CAN import:
- `*.domain.*` (domain layer)
- `org.jetbrains.androidx.lifecycle.*` (ViewModel)
- `kotlinx.coroutines.*`
- `org.koin.*` (for module declarations)

### presentation/ CANNOT import:
- `*.data.*`
- `io.ktor.*`, `io.github.jan.supabase.*`
- `kotlinx.serialization.*`
- `android.*` (in commonMain)

## Prohibited Patterns

### 1. Business Logic in ViewModel
```kotlin
// WRONG
class WorkoutViewModel : ViewModel() {
    fun save(workout: Workout) {
        if (workout.exercises.isEmpty()) { /* validation */ }  // Business logic!
        repository.save(workout)  // Direct repo call!
    }
}

// CORRECT
class WorkoutViewModel(
    private val saveWorkoutUseCase: SaveWorkoutUseCase,
) : ViewModel() {
    fun save(workout: Workout) {
        viewModelScope.launch {
            saveWorkoutUseCase(workout)  // Use case handles validation
        }
    }
}
```

### 2. Direct Repository Calls from UI
```kotlin
// WRONG — skips domain layer
@Composable
fun WorkoutScreen(repository: WorkoutRepository) { ... }

// CORRECT — always through ViewModel + UseCase
@Composable
fun WorkoutRoute(viewModel: WorkoutViewModel = koinViewModel()) { ... }
```

### 3. Framework Types in Domain
```kotlin
// WRONG
interface WorkoutRepository {
    fun getWorkouts(): LiveData<List<Workout>>  // Android framework type
}

// CORRECT
interface WorkoutRepository {
    fun getWorkouts(): Flow<List<Workout>>  // Pure Kotlin
}
```

### 4. Supabase in Domain
```kotlin
// WRONG
class GetWorkoutsUseCase(
    private val supabase: SupabaseClient,  // Framework in domain!
) { ... }

// CORRECT
class GetWorkoutsUseCase(
    private val repository: WorkoutRepository,  // Interface only
) { ... }
```

### 5. Leaking Data Types
```kotlin
// WRONG — DTO leaks to presentation
class WorkoutViewModel(
    private val getWorkoutsUseCase: GetWorkoutsUseCase,
) : ViewModel() {
    val uiState: StateFlow<List<WorkoutDto>>  // DTO in presentation!
}

// CORRECT — domain entity only
class WorkoutViewModel(...) : ViewModel() {
    val uiState: StateFlow<WorkoutListUiState>  // UiState with domain entities
}
```

## File Organization

```
shared/src/commonMain/kotlin/com/strakk/shared/
  domain/
    model/            # Entities, value objects
    repository/       # Repository interfaces
    usecase/          # Use cases
    error/            # Domain errors (sealed interfaces)
  data/
    repository/       # Repository implementations (internal)
    remote/           # Remote data sources, DTOs (internal)
    local/            # Local data sources, entities (internal)
    mapper/           # Mappers (internal)
    di/               # Koin modules
  presentation/
    feature/          # Per-feature: ViewModel, UiState, Event
    di/               # Koin modules
```

## Konsist Enforcement (CI)

```kotlin
@Test
fun `domain depends on nothing`() {
    Konsist.scopeFromModule("shared")
        .files
        .filter { it.resideInPackage("com.strakk.shared.domain..") }
        .assertFalse {
            it.hasImport { import ->
                import.name.startsWith("com.strakk.shared.data") ||
                import.name.startsWith("com.strakk.shared.presentation")
            }
        }
}

@Test
fun `data layer classes are internal`() {
    Konsist.scopeFromModule("shared")
        .classes()
        .filter { it.resideInPackage("com.strakk.shared.data..") }
        .withoutModifier(KoModifier.DATA)
        .assertTrue { it.hasInternalModifier }
}
```

## References

- For good vs bad examples for each architecture rule, see [references/layer-examples.md](references/layer-examples.md)
- For complete Konsist enforcement tests, see [references/konsist-tests.kt](references/konsist-tests.kt)
