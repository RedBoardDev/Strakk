# Strakk

[![CI](https://github.com/RedBoardDev/Strakk/actions/workflows/ci.yml/badge.svg)](https://github.com/RedBoardDev/Strakk/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Personal nutrition tracking and weekly check-in app built with Kotlin Multiplatform. Shared business logic, native UI on both platforms.

## Features

- Meal logging with AI-powered photo scanning and food recognition
- Macro tracking (calories, protein, carbs, fat) with daily summaries
- Weekly check-ins with body measurements, mood, and progress photos
- AI-generated nutrition goals based on user profile
- Water intake tracking
- Food search across local catalog and Open Food Facts
- Workout PDF parsing and Hevy export
- Pro subscription with RevenueCat billing

## Architecture

Kotlin Multiplatform (KMP) with Clean Architecture:

| Module | Description |
|--------|-------------|
| [`shared/`](shared/) | Kotlin — domain, data, presentation (compiled for iOS and Android) |
| [`androidApp/`](androidApp/) | Jetpack Compose, Material 3 |
| [`iosApp/`](iosApp/) | SwiftUI, iOS 17+ |
| [`supabase/`](supabase/) | Postgres migrations and Deno Edge Functions |
| [`infra/nutrition-api/`](infra/nutrition-api/) | Self-hosted Deno API for meal scanning (vision + vector search) |
| [`scripts/`](scripts/) | Dev utilities (backtest, import, seed) |

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full architecture diagram and layer rules.

## Stack

- **Shared**: Kotlin 2.1, supabase-kt 3.1, Ktor, Koin, kotlinx.serialization, SKIE
- **iOS**: Swift 6, SwiftUI, iOS 17+
- **Android**: Jetpack Compose, Material 3, API 26+
- **Backend**: Supabase (Postgres, Auth, Storage, Edge Functions)
- **Infra**: Deno, Docker, Qdrant (vector search)

## Prerequisites

- JDK 17+ (`brew install openjdk@17`)
- Android Studio (Ladybug or later)
- Xcode 15+ (macOS only, for iOS)
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`)
- Deno 2.x (`brew install deno`) — for edge function linting
- [Lefthook](https://github.com/evilmartians/lefthook) (`brew install lefthook`) — pre-commit hooks

## Setup

```bash
# 1. Clone
git clone https://github.com/RedBoardDev/Strakk.git
cd Strakk

# 2. Configure environment
cp local.properties.example local.properties   # Android — fill in Supabase keys
mkdir -p iosApp/Config
# Create Production.xcconfig and Staging.xcconfig from the .example files

# 3. Install pre-commit hooks
make setup

# 4. Build
make build          # Android debug APK
make ios-project    # Generate Xcode project
```

## Build and Test

```bash
# All linters
make lint

# Kotlin (Detekt)
make lint-kotlin

# Deno (edge functions)
make lint-deno

# SwiftLint (local macOS only)
make lint-swift

# Shared tests (JVM + iOS simulator)
./gradlew :shared:allTests

# Android debug APK
./gradlew :androidApp:assembleDebug

# iOS framework
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Full check (lint + test + build)
make check
```

## Project Structure

```
Strakk/
├── shared/src/commonMain/kotlin/com/strakk/shared/
│   ├── domain/          # Models, repository interfaces, use cases (zero deps)
│   ├── data/            # Repository impls, DTOs, mappers (internal)
│   ├── presentation/    # ViewModels, UiState, Effects
│   └── di/              # Koin modules
├── androidApp/          # Jetpack Compose screens (Route/Screen/Content)
├── iosApp/              # SwiftUI views, @Observable ViewModel wrappers
├── supabase/
│   ├── migrations/      # Postgres DDL
│   └── functions/       # Deno Edge Functions
├── infra/nutrition-api/ # Self-hosted meal scanning API
├── scripts/             # Dev utilities
├── docs/                # Architecture, contributing, security
├── config/detekt/       # Detekt configuration
└── gradle/              # Version catalog (libs.versions.toml)
```

## Contributing

See [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) for development workflow, code style, and PR guidelines.

## Security

To report a vulnerability, see [`docs/SECURITY.md`](docs/SECURITY.md).

## License

This project is licensed under the MIT License. See [`LICENSE`](LICENSE) for details.
