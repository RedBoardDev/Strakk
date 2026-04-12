# Strakk

Personal fitness & nutrition tracking app.

## Architecture

Kotlin Multiplatform (KMP) with Clean Architecture:
- **shared/** — Kotlin (domain, data, presentation) — compiled for iOS & Android
- **iosApp/** — Swift/SwiftUI (native Apple UI)
- **androidApp/** — Kotlin/Jetpack Compose (native Material 3 UI)

## Stack

- **Backend**: Supabase (Postgres, Auth, Storage, Edge Functions)
- **Shared**: Kotlin 2.1, Ktor, Koin, kotlinx.serialization
- **iOS**: Swift 6, SwiftUI, iOS 17+
- **Android**: Jetpack Compose, Material 3, API 26+

## Build

### Prerequisites
- JDK 17+
- Android Studio
- Xcode 15+

### Android
```
./gradlew :androidApp:assembleDebug
```

### iOS
```
# Generate Xcode project
cd iosApp && xcodegen generate

# Build frameworks
./gradlew :shared:linkDebugFrameworkIosArm64
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Open in Xcode
open iosApp/Strakk.xcodeproj
```

### Both
```
./gradlew :shared:build :androidApp:assembleDebug
```

## Project Structure

```
Strakk/
├── shared/src/commonMain/kotlin/com/strakk/shared/
│   ├── domain/          ← Models, repository interfaces, use cases
│   ├── data/            ← Repository implementations, DTOs, Ktor client
│   ├── presentation/    ← ViewModels, UiState
│   └── di/              ← Koin modules
├── iosApp/              ← SwiftUI views, ViewModel wrappers
├── androidApp/          ← Jetpack Compose screens
└── docs/                ← Feature specs, architecture docs
```
