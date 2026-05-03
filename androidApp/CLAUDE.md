# androidApp/ — Jetpack Compose App

Compose + Material 3, API 26+. Skill: `compose-conventions` (read before implementing).
Design: `DESIGN.md` at project root — source of truth for every screen.

## Structure
```
src/main/kotlin/com/strakk/android/
  MainActivity.kt
  features/
    auth/          # LoginRoute, LoginScreen
    onboarding/    # OnboardingFlowRoute + 8 step content composables
    home/          # TodayRoute, TodayScreen (2×2 macro grid)
    root/          # RootContent
    components/    # OnboardingProgressBar, StepperRow, SelectableCard, PillSelector, ChipGrid
  ui/theme/        # Material 3 theme (Color, Type, Shape)
```

## Key Patterns
- **Route/Screen/Content**: Route collects state & handles nav, Screen is the stateful composable, Content is stateless/previewable
- `collectAsStateWithLifecycle()` — never `collectAsState()`
- `koinViewModel()` for ViewModel injection
- `@Serializable` data classes for navigation routes
- Edge-to-edge: `Scaffold` with `innerPadding` or `WindowInsets.safeDrawing`

## Environments
Two flavors: `prod` and `staging`. See `ENVIRONMENTS.md`.
Build: `./gradlew :androidApp:assembleDebug`

## Localization
- English strings in `res/values/strings.xml`
- French strings in `res/values-fr/strings.xml`
- Use `stringResource(R.string.key)` in composables
- **Never** write French directly in `.kt` files
- Key naming: `snake_case`, prefixed by screen — `paywall_cta`, `checkin_empty_title`

## Lint
`make lint-kotlin` (Detekt strict, zero tolerance). Runs in CI.
