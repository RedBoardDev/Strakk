# Rule — No dead code, no duplication

The repo must be free of code that no live entrypoint reaches, and free of repeated logic that could be extracted.

## Hard rules

### Dead code
- No unused imports.
- No unused private members (functions, properties, classes).
- No unused public symbols (verified by cross-file grep across `shared/`, `androidApp/`, `iosApp/`).
- No orphan files (zero references from any compilation unit).
- No unused string resources, drawable resources, mipmap resources.
- No commented-out code blocks in committed files. If something is "for later", it goes in a TODO comment with a ticket reference, not as dead code.

### Duplication
- No identical 30+ line blocks duplicated across files.
- Repository error-handling boilerplate (try/catch around supabase calls) extracted to a single helper.
- Mappers either share base logic or are intentionally distinct (and that intent is documented if non-obvious).
- Conceptual duplicates (two ways to format the same thing, two error types representing the same failure) are consolidated.

## Exceptions

- **Test fixtures** may duplicate setup — that's acceptable up to a point. Egregious duplication in tests is still flagged.
- **Cross-platform UI** (iOS Swift vs Android Compose) cannot share UI code. Their **constants** (paddings, sizes, colors, copy keys) should live in `shared/` design specs.
- **Generated code** is not duplication.
- **WIP code** marked with a clear `// TODO(<owner>): <ticket>` is acceptable for a release cycle but should be cleaned up before the next.

## How to apply

1. **When writing**: stop and check before adding a new utility. Search for the same name or close variants. If it exists, use it.
2. **When refactoring**: extract on the third repetition. Two is coincidence, three is a pattern.
3. **When reviewing**: every new function should justify why an existing helper isn't enough.
4. **In CI**: `dead-code-finder` and `duplicate-finder` agents run before any release-readiness gate closes.

## Tooling

- **Detekt** catches per-file unused (configured in `config/detekt/detekt.yml`).
- **SwiftLint** catches some unused declarations.
- **`deno lint`** catches unused TS imports/vars.
- **`jscpd`** (cross-language CPD) — not installed by default; recommended for periodic deep audits.
- **`Periphery`** (iOS-specific dead-code finder) — recommended for iOS deep audits.
