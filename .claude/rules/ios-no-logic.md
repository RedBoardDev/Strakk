---
description: "iOS views must not contain business logic or data layer imports"
globs:
  - "iosApp/**/*.swift"
---
Swift files must NOT:
- Import `com.strakk.shared.data` (only `shared` framework import allowed)
- Contain business logic (calculations, validation, data transformation)
- Call Supabase, Ktor, or any network API directly

All logic flows through the KMP ViewModel obtained via KoinHelper.
SwiftUI views only handle: layout, navigation, user interaction, platform APIs (camera, notifications).
