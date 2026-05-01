---
description: "ViewModels must use UseCases, never Repositories directly"
paths:
  - "shared/src/commonMain/kotlin/com/strakk/shared/presentation/**/*.kt"
---
# Presentation Uses Use Cases Only

- ViewModels depend on use cases, never repositories.
- Expose immutable `StateFlow<UiState>`. Events/effects use `sealed interface`.
- Must not import `com.strakk.shared.data.*`, Supabase, Ktor, or serialization.

See `architecture-rules` skill for full patterns.
