---
description: "Data layer visibility rules"
paths:
  - "shared/src/commonMain/kotlin/com/strakk/shared/data/**/*.kt"
---
# Data Layer Encapsulation

All data layer classes are `internal` — public API stays in `domain/repository`.

- DTOs use `@Serializable` + `@SerialName("snake_case")` for Supabase fields.
- Map DTOs to domain models before returning from repositories.
- Data may import domain, Supabase, Ktor, serialization, Koin — never presentation or UI.

See `architecture-rules` skill for full rules.
