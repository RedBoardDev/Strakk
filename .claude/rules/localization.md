# Localization Convention

All user-facing strings MUST go through the platform localization system. Never hardcode translated text in source code.

## iOS (SwiftUI)
- Source language is **English** in code: `Text("Start now")`
- Xcode auto-extracts strings to `Localizable.xcstrings` (String Catalog)
- French translations are added in the String Catalog under the `fr` locale
- Use `String(localized:)` for programmatic strings: `String(localized: "Start now")`
- String interpolation uses `\(variable)` — Xcode handles positional arguments
- Never write French directly in `.swift` files

## Android (Compose)
- English strings in `res/values/strings.xml`
- French strings in `res/values-fr/strings.xml`
- Use `stringResource(R.string.key)` in composables
- Never write French directly in `.kt` files

## Shared (KMP)
- Domain model strings (feature names, descriptions in `ProFeatureInfo`) stay in Kotlin
- These are displayed via native UI and should be localized per platform if needed
- Error messages from domain use English; UI layer maps to localized strings

## Naming Convention
- iOS: Xcode uses the English string as key (no separate key needed)
- Android: `snake_case` keys prefixed by screen: `paywall_cta`, `checkin_empty_title`
