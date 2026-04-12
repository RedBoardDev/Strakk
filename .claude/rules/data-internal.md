---
description: "Data layer visibility rules"
globs:
  - "shared/src/commonMain/kotlin/com/strakk/shared/data/**/*.kt"
---
All classes in the data layer must be `internal`. Only domain interfaces are public.
DTOs must be `@Serializable` and `internal`.
Repository implementations must be `internal class`.
