---
description: "SwiftUI/iOS conventions for iosApp"
paths:
  - "iosApp/**/*.swift"
---

# SwiftUI / iOS Conventions for Strakk

## KMP ViewModel Bridge Pattern

```swift
@MainActor
@Observable
final class FeatureViewModelWrapper {
    private let sharedVm: FeatureViewModel
    var state: FeatureUiState

    private var observationTask: Task<Void, Never>?

    init(sharedVm: FeatureViewModel) {
        self.sharedVm = sharedVm
        self.state = sharedVm.uiState.value

        observationTask = Task {
            for await newState in sharedVm.uiState {
                self.state = newState
            }
        }
    }

    deinit { observationTask?.cancel() }

    func onEvent(_ event: FeatureEvent) {
        sharedVm.onEvent(event: event)
    }
}
```

### Key rules:
- `@Observable` (NOT `ObservableObject` / `@Published`)
- `@MainActor` on ALL ViewModel wrappers
- Cancel observation tasks in `deinit`
- Use SKIE for `sealed interface` -> Swift `enum` bridging
- Use `onEnum(of:)` for exhaustive switch on SKIE-bridged types

## View Patterns

### Async Loading
```swift
// CORRECT
.task { await viewModel.loadData() }

// WRONG — unstructured concurrency
.onAppear { Task { await viewModel.loadData() } }
```

### Navigation
```swift
// CORRECT
NavigationStack(path: $router.path) { ... }

// WRONG — deprecated & removed in iOS 26
NavigationView { ... }
```

### Sheets
```swift
// PREFERRED — data-driven
.sheet(item: $selectedWorkout) { workout in DetailView(workout: workout) }

// AVOID — boolean-driven
.sheet(isPresented: $showDetail) { ... }
```

### Lists
```swift
// Large collections
ScrollView {
    LazyVStack(spacing: 12) {
        ForEach(workouts) { workout in
            WorkoutRow(workout: workout)
        }
    }
}

// NOT VStack for large datasets
```

## Icons

- SF Symbols for ALL icons: `Image(systemName: "figure.run")`
- Never bundle custom icons when SF Symbol exists

## iOS 26 / Liquid Glass Design (WWDC 2025)

### Liquid Glass — When to Use

Liquid Glass is the new adaptive material for **navigation-layer controls** that float above content.
Use it for: floating action buttons, toolbar overlays, custom bottom bars, card overlays on media backgrounds.
Do NOT apply it to content-level views (list rows, form fields, plain text).

```swift
// Floating action button with glass effect
Button { viewModel.onEvent(.addSet) } label: {
    Image(systemName: "plus")
        .font(.title2)
        .padding()
}
.glassEffect(.regular, in: .circle)

// Tinted glass for contextual actions
Button("Start Workout") { ... }
    .padding(.horizontal, 20)
    .padding(.vertical, 12)
    .glassEffect(.regular.tint(.green).interactive(), in: .capsule)
```

### Glass Variants
- `.regular` — standard translucent glass (default, use for most controls)
- `.clear` — minimal glass, lighter effect
- Use `.tint(_:)` for semantic coloring (e.g., green for "start", red for "stop")
- Use `.interactive()` for press/hover feedback (scale, bounce, shimmer)

### GlassEffectContainer — Grouping Glass Elements
When glass elements are close together, wrap them in `GlassEffectContainer` so they
blend and morph together (like water droplets merging).

```swift
GlassEffectContainer(spacing: 40) {
    HStack(spacing: 16) {
        Button("Rest Timer") { ... }
            .glassEffect(.regular, in: .capsule)
        Button("Add Set") { ... }
            .glassEffect(.regular, in: .capsule)
    }
}
```

### Backward Compatibility
Always gate iOS 26 APIs behind availability checks:
```swift
if #available(iOS 26, *) {
    content.glassEffect(.regular, in: .capsule)
} else {
    content.background(.ultraThinMaterial, in: .capsule)
}
```

### TabView — iOS 26 Liquid Glass Tab Bar

Tab bars automatically get Liquid Glass styling when compiled with Xcode 26.
New APIs to control tab bar behavior:

```swift
TabView(selection: $selectedTab) {
    Tab("Home", systemImage: "house", value: .home) {
        HomeScreen()
    }
    Tab("Sessions", systemImage: "figure.strengthtraining.traditional", value: .sessions) {
        SessionListScreen()
    }
}
// Minimize tab bar on scroll (great for workout detail screens)
.tabBarMinimizeBehavior(.onScrollDown)
// Optional: accessory above tab bar (e.g., active workout timer)
.tabViewBottomAccessory {
    ActiveWorkoutBar()
}
```

### backgroundExtensionEffect — Immersive Backgrounds
Extends a view's background beyond the safe area with a mirrored blur.
Useful for workout hero images or progress visualizations.

```swift
Image("workout-hero")
    .resizable()
    .aspectRatio(contentMode: .fill)
    .backgroundExtensionEffect()
```

### ToolbarSpacer — Better Toolbar Layout
Group toolbar items with explicit spacing:

```swift
.toolbar {
    ToolbarItem(placement: .primaryAction) {
        Button("Save") { ... }
    }
    ToolbarSpacer(.fixed)
    ToolbarItem(placement: .primaryAction) {
        Button("Share") { ... }
    }
}
```

### Swift Charts — Scrollable Charts (iOS 26)
For workout history / progress tracking with large datasets:

```swift
Chart(workoutHistory) { entry in
    BarMark(
        x: .value("Date", entry.date),
        y: .value("Volume", entry.totalVolume)
    )
}
.chartScrollableAxes(.horizontal)
.chartXVisibleDomain(length: 7) // Show 7 days at a time
```

## Swift Concurrency

- Prefer structured concurrency (child tasks, task groups)
- `@MainActor` for UI-bound types
- `nonisolated` or `@concurrent` for background work
- NEVER `@unchecked Sendable` without justification
- Detect Swift 6 language mode in Package.swift/project settings

## File Organization

- One type per file
- Separate View files from ViewModel wrapper files
- Group by feature, not by type

## Accessibility

- VoiceOver labels on all interactive elements
- Dynamic Type support (no fixed font sizes)
- 4.5:1 minimum contrast ratio
- `accessibilityLabel`, `accessibilityHint` on buttons
- Reduced motion alternatives via `@Environment(\.accessibilityReduceMotion)`

## References

- For complete ViewModel wrapper template, see [references/viewmodel-wrapper-pattern.swift](references/viewmodel-wrapper-pattern.swift)
- For navigation patterns (NavigationStack, tabs, sheets, deep linking), see [references/navigation-patterns.swift](references/navigation-patterns.swift)
- For Liquid Glass patterns and iOS 26 design, see [references/liquid-glass-patterns.swift](references/liquid-glass-patterns.swift)
