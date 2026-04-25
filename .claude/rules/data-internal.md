---
description: "Data layer visibility rules"
paths:
  - "shared/src/commonMain/kotlin/com/strakk/shared/data/**/*.kt"
---
# Data Layer Encapsulation

The data layer implements domain contracts and hides all infrastructure details.

- Data classes, DTOs, mappers, remote/local data sources, and repository implementations are `internal`.
- Public API stays in `domain/repository`.
- DTOs use `@Serializable` and `@SerialName("snake_case")` for Supabase fields.
- Repository implementations map DTOs to domain models before returning.
- Data may import domain, Supabase, Ktor, serialization, and Koin module APIs.
- Data must never import presentation or UI code.
