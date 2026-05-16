# Kotlin Lint Rules (Detekt + ktlint)

Every Kotlin file in `shared/` MUST pass `make lint-kotlin` (Detekt strict, zero tolerance).
Apply these rules while writing code — not as a fix-up pass afterwards.

## Import ordering
- ALL imports sorted lexicographically (LC_ALL=C sort order)
- NO blank lines between imports
- `kotlin.*` comes before `kotlinx.*` alphabetically
- No unused imports — ever

## Naming
- Variables: `[a-z][A-Za-z0-9]*` — minimum 2 chars. Never `m`, `s`, `v` alone.
- Constants: `[A-Z][A-Za-z0-9_]*` — use `UPPER_SNAKE` for `const val`
- Functions: `[a-zA-Z][a-zA-Z0-9]*`

## Formatting
- Max line length: **120 chars** (excluding imports, package, comments, raw strings)
- Trailing commas on all multiline call sites and declaration sites
- Function signature: if params fit on one line ≤120 chars, keep them on one line
- Expression body (`= expr`): if the first line of the body fits on the signature line, put it there
- Multi-line if/else: always use braces `{ }`

## Complexity thresholds
- `LongMethod`: max 60 lines
- `LongParameterList`: max 6 for functions, max 8 for constructors
- `CyclomaticComplexMethod`: max 15
- `TooManyFunctions`: max 20 per file, 15 per class
- `NestedBlockDepth`: max 4
- `ReturnCount`: max 3 (guard clauses excluded)

## When thresholds are exceeded
- Add `@Suppress("RuleName")` on the specific symbol — not file-wide
- Exception: HTML/template builders (`data/pdf/`) may use `@file:Suppress` for `MagicNumber`, formatting rules

## Exception handling
- `catch (_: Exception)` or `catch (e: Exception)` — both accepted
- The `_` or `e` prefix patterns are allowed by config
- Never catch `CancellationException` silently — always rethrow

## Magic numbers
- Use named constants or `ignoreNamedArgument` — don't inline raw numbers
- Exception: `data/pdf/` directory (SVG coordinates, CSS values)
