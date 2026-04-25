---
description: "Enforce domain layer purity"
paths:
  - "shared/src/commonMain/kotlin/com/strakk/shared/domain/**/*.kt"
---
# Domain Layer Purity

Domain is pure business logic and depends on nothing in the app.

- Allowed imports: `kotlin.*`, `kotlinx.coroutines.*`, `kotlinx.datetime.*`.
- Forbidden imports: `com.strakk.shared.data.*`, `com.strakk.shared.presentation.*`, `io.ktor.*`, `io.github.jan.supabase.*`, `org.koin.*`, `android.*`, `androidx.*`, `platform.*`, `kotlinx.serialization.*`.
- Repository types are interfaces only.
- Use cases expose `operator fun invoke(...)`.
- Domain errors are `sealed interface` hierarchies.
- Domain models must not contain DTO, Supabase, Ktor, UI, or platform concepts.
