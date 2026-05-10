# androidApp/

Native Android app built with Jetpack Compose and Material 3.

## Architecture

Follows the **Route / Screen / Content** pattern:

- **Route**: Composable that collects ViewModel state with lifecycle awareness
- **Screen**: Stateful composable that receives state and event callbacks
- **Content**: Stateless composable for pure UI rendering

ViewModels come from `shared/presentation/` — this module contains no business logic.

## Key conventions

- Use `collectAsStateWithLifecycle()`, never `collectAsState()`
- All strings in `res/values/strings.xml` (English) and `res/values-fr/strings.xml` (French)
- Use `stringResource(R.string.key)` — never hardcode text
- Never import from `shared/data/` (it is `internal`)
- Follow Material 3 design tokens and `DESIGN.md`

## Build

```bash
# Debug APK
./gradlew :androidApp:assembleDebug

# Release APK (unsigned)
./gradlew :androidApp:assembleRelease

# Prod variant
./gradlew :androidApp:assembleProdDebug

# Staging variant
./gradlew -Penv=staging :androidApp:assembleStagingDebug
```

## Environment

The app reads Supabase credentials from `local.properties` at the project root (gitignored). See [`docs/ENVIRONMENTS.md`](../docs/ENVIRONMENTS.md).

## Requirements

- JDK 17+
- Android SDK (API 26+ target, compile SDK 35)
- Android Studio Ladybug or later
