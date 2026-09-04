# Rule — Architecture boundaries are not negotiable

The Clean Architecture layers in `shared/` are load-bearing. Crossing them is a critical bug, not a style issue.

## The rules (compact)

| Layer | Can import from | Cannot import from |
|-------|-----------------|--------------------|
| `domain/` | nothing (pure Kotlin stdlib + kotlinx libs only) | `data/`, `presentation/`, Ktor, supabase-kt, Android, Foundation, UIKit |
| `data/` | `domain/` | `presentation/` |
| `presentation/` | `domain/` | `data/`, Android, UIKit |
| `androidApp/` | `presentation/`, `domain/` | `data/` (data is `internal` — won't compile if attempted) |
| `iosApp/` | `presentation/`, `domain/` (via SKIE) | `data/` |

## Visibility

- `data/` classes, top-level functions, properties, and DTOs are `internal` unless DI explicitly requires `public` (rare; document why).
- `domain/` is `public` (or default).
- `presentation/` ViewModels are `public` (consumed by both platforms).

## ViewModel discipline

- A ViewModel injects **UseCase** types, never `Repository` directly.
- A ViewModel does no I/O — UseCases handle that.
- A ViewModel's state is a single immutable `data class` updated via `MutableStateFlow`.
- A ViewModel's effects are a `SharedFlow<Effect>` of a sealed interface.
- A ViewModel never references Android-specific or iOS-specific types.

## Errors across layers

- `data/` catches platform exceptions (`HttpRequestException`, `ConnectTimeoutException`, etc.) and maps them to **domain errors** (sealed types in `domain/`).
- `presentation/` consumes domain errors and translates them to UI state.
- The UI layer (Compose / SwiftUI) does not pattern-match on Ktor exception types.

## When you find a violation

1. Stop. The violation is a CRITICAL finding, not a stylistic one.
2. Surface it via the `architecture-reviewer` audit.
3. Propose a refactor (move file, split, introduce abstraction) — do not paper over with a `@Suppress` or comment.
4. After the fix, the relevant layer's import grep must return zero forbidden imports:
   ```bash
   grep -rE '^import (com\.strakk\.shared\.(data|presentation)|io\.ktor|io\.github\.jan\.supabase|android|UIKit)' shared/src/commonMain/kotlin/com/strakk/shared/domain/
   ```

## Why these rules

- **Domain purity** lets you change the SDK (supabase-kt → Ktor-direct, Postgrest → REST, etc.) without touching domain logic.
- **Data encapsulation** prevents accidental coupling — UI cannot depend on a DTO field name; if the DTO changes, only the mapper changes.
- **ViewModel discipline** keeps business rules in UseCases (testable in `commonTest`) instead of in ViewModels (harder to test).
- **Cross-layer error mapping** stops platform leaks — a ViewModel matching on `HttpRequestException` couples the UI to a specific HTTP client.
