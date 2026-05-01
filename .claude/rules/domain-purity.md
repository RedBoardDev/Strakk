---
description: "Enforce domain layer purity"
paths:
  - "shared/src/commonMain/kotlin/com/strakk/shared/domain/**/*.kt"
---
# Domain Layer Purity

Domain is pure business logic — zero dependencies on data, presentation, or frameworks.

- Allowed imports: `kotlin.*`, `kotlinx.coroutines.*`, `kotlinx.datetime.*`.
- Repository types are interfaces only. Use cases expose `operator fun invoke(...)`.
- Domain errors are `sealed interface` hierarchies, not sealed classes.

See `architecture-rules` skill for full import rules and layer examples.
