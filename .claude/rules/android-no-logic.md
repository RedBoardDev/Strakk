---
description: "Android Composables must not contain business logic or data layer imports"
globs:
  - "androidApp/**/*.kt"
---
Composable functions must NOT:
- Import from `com.strakk.shared.data` (only `com.strakk.shared.presentation` and `com.strakk.shared.domain.model`)
- Contain business logic (calculations, validation, data transformation)
- Call Supabase, Ktor, or any network API directly

All logic flows through the KMP ViewModel obtained via koinViewModel().
Compose screens only handle: layout, navigation, user interaction, platform APIs (camera, notifications).
