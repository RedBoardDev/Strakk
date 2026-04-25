---
description: "No platform-specific code in commonMain"
paths:
  - "shared/src/commonMain/**/*.kt"
---
# No Platform Code In commonMain

Code in `shared/src/commonMain` must stay multiplatform.

- Forbidden imports: `android.*`, `androidx.*`, `platform.UIKit.*`, `platform.Foundation.*`, `java.io.*`, `java.time.*`.
- Use `expect`/`actual` or an interface bound through Koin for platform-specific needs.
- Use `kotlinx.datetime`, not `java.time`.
- Use Kotlin/Multiplatform-safe APIs only.
- If an API cannot compile for iOS, it does not belong in `commonMain`.
