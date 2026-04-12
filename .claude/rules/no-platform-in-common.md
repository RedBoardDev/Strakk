---
description: "No platform-specific code in commonMain"
globs:
  - "shared/src/commonMain/**/*.kt"
---
Code in commonMain must NOT import:
- `android.*` or `androidx.*`
- `platform.UIKit.*` or `platform.Foundation.*`
- `java.io.*` or `java.time.*`
Use expect/actual for platform-specific needs.
Use kotlinx.datetime instead of java.time.
Use kotlinx.io instead of java.io.
