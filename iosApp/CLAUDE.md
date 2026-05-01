# iosApp/ — SwiftUI App

Swift 6, SwiftUI, iOS 17+. Skill: `swiftui-conventions` (read before implementing).
Design: `DESIGN.md` at project root — source of truth for every screen.

## Structure
```
Sources/Features/
  Auth/          # LoginView + LoginViewModelWrapper
  Onboarding/    # OnboardingFlowView + 8 step views (Welcome, Weight, Bio, Goal, Activity, SignUp, Goals, DayPreview)
  Home/          # TodayView (2×2 macro grid)
  Root/          # RootView + RootViewModelWrapper
  Components/    # OnboardingProgressBar, StepperRow, SelectableCard, PillSelector, ChipGrid
```

## Key Patterns
- ViewModel wrappers: `@Observable @MainActor class XxxViewModelWrapper` — bridges KMP ViewModel
- KMP StateFlow → Swift via SKIE `AsyncSequence`: `for await state in viewModel.stateFlow { ... }`
- Navigation: `NavigationStack` with `.navigationDestination`
- Async: `.task { }` modifier, never `DispatchQueue`
- Icons: SF Symbols only

## Environments
Two schemes: **Strakk** (prod) and **Strakk Dev** (staging) via `Config/Production.xcconfig` / `Config/Staging.xcconfig` (gitignored — see `ENVIRONMENTS.md`).
Regenerate Xcode project: `cd iosApp && xcodegen generate`

## Lint
`make lint-swift` (local only, not in CI). Config: `iosApp/.swiftlint.yml`.
