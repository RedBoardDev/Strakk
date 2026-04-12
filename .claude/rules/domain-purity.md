---
description: "Enforce domain layer purity"
globs:
  - "shared/src/commonMain/kotlin/com/strakk/shared/domain/**/*.kt"
---
Files in the domain layer must NOT import:
- Any package from `com.strakk.shared.data`
- Any package from `com.strakk.shared.presentation`
- Any framework (Ktor, Supabase, Koin, Android, iOS)
- Only stdlib, kotlinx.coroutines, and kotlinx.datetime are allowed
- kotlinx.serialization is NOT allowed (serialization is a data layer concern)
