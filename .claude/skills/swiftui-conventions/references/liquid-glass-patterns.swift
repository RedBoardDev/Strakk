import SwiftUI

// =============================================================================
// Pattern 1: Basic Liquid Glass Button
// =============================================================================

/// Floating action button with glass effect.
/// Use for primary actions that float above content (e.g., "Add Set", "Start Workout").
@available(iOS 26, *)
struct GlassFloatingButton: View {
    let systemImage: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.title2)
                .padding()
        }
        .glassEffect(.regular, in: .circle)
    }
}

// =============================================================================
// Pattern 2: Tinted Interactive Glass Button
// =============================================================================

/// Use `.tint(_:)` for semantic meaning and `.interactive()` for press feedback.
/// Green = start/go, Red = stop/delete, default = neutral action.
@available(iOS 26, *)
struct TintedGlassButton: View {
    let title: String
    let tintColor: Color
    let action: () -> Void

    var body: some View {
        Button(title, action: action)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .glassEffect(
                .regular.tint(tintColor).interactive(),
                in: .capsule
            )
    }
}

// Usage:
// TintedGlassButton(title: "Start Workout", tintColor: .green) { ... }
// TintedGlassButton(title: "Stop", tintColor: .red) { ... }

// =============================================================================
// Pattern 3: GlassEffectContainer — Grouped Glass Elements
// =============================================================================

/// When multiple glass elements are close together, wrap them in GlassEffectContainer
/// so they blend and morph together (like water droplets merging).
@available(iOS 26, *)
struct WorkoutActionBar: View {
    let onRestTimer: () -> Void
    let onAddSet: () -> Void
    let onFinish: () -> Void

    var body: some View {
        GlassEffectContainer(spacing: 40) {
            HStack(spacing: 16) {
                Button("Rest Timer", action: onRestTimer)
                    .glassEffect(.regular, in: .capsule)

                Button("Add Set", action: onAddSet)
                    .glassEffect(.regular, in: .capsule)

                Button(action: onFinish) {
                    Image(systemName: "checkmark")
                        .font(.title3)
                        .padding(12)
                }
                .glassEffect(.regular.tint(.green).interactive(), in: .circle)
            }
        }
    }
}

// =============================================================================
// Pattern 4: TabView with Liquid Glass Tab Bar
// =============================================================================

/// Tab bars automatically get Liquid Glass when compiled with Xcode 26.
/// Use tabBarMinimizeBehavior for immersive content screens.
@available(iOS 26, *)
struct StrakkTabView: View {
    @State private var selectedTab: AppTab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            Tab("Home", systemImage: "house", value: .home) {
                HomeScreen()
            }
            Tab("Sessions", systemImage: "figure.strengthtraining.traditional", value: .sessions) {
                SessionListScreen()
            }
            Tab("Progress", systemImage: "chart.line.uptrend.xyaxis", value: .progress) {
                ProgressScreen()
            }
            Tab("Profile", systemImage: "person", value: .profile) {
                ProfileScreen()
            }
        }
        // Minimize tab bar on scroll (great for workout detail screens)
        .tabBarMinimizeBehavior(.onScrollDown)
        // Optional: accessory above tab bar (e.g., active workout timer)
        .tabViewBottomAccessory {
            ActiveWorkoutBar()
        }
    }
}

enum AppTab: Hashable {
    case home, sessions, progress, profile
}

// =============================================================================
// Pattern 5: Backward Compatibility
// =============================================================================

/// Always gate iOS 26 APIs behind availability checks.
/// Fall back to .ultraThinMaterial for iOS 17/18.
struct CompatibleGlassButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(title, action: action)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .modifier(GlassOrMaterialModifier())
    }
}

struct GlassOrMaterialModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26, *) {
            content.glassEffect(.regular, in: .capsule)
        } else {
            content.background(.ultraThinMaterial, in: .capsule)
        }
    }
}

// =============================================================================
// Pattern 6: backgroundExtensionEffect — Hero Images
// =============================================================================

/// Extends background beyond safe area with a mirrored blur.
/// Use for workout hero images or progress visualizations.
@available(iOS 26, *)
struct WorkoutHeroHeader: View {
    let imageName: String

    var body: some View {
        Image(imageName)
            .resizable()
            .aspectRatio(contentMode: .fill)
            .frame(height: 200)
            .clipped()
            .backgroundExtensionEffect()
    }
}

// =============================================================================
// Pattern 7: ToolbarSpacer — Grouped Toolbar Items
// =============================================================================

/// Use ToolbarSpacer to group toolbar items with explicit spacing.
@available(iOS 26, *)
struct SessionDetailToolbar: ViewModifier {
    let onSave: () -> Void
    let onShare: () -> Void

    func body(content: Content) -> some View {
        content.toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Save", action: onSave)
            }
            ToolbarSpacer(.fixed)
            ToolbarItem(placement: .primaryAction) {
                Button(action: onShare) {
                    Image(systemName: "square.and.arrow.up")
                }
            }
        }
    }
}

// =============================================================================
// Pattern 8: Scrollable Charts (iOS 26)
// =============================================================================

/// For workout history / progress tracking with large datasets.
/// chartScrollableAxes enables horizontal panning, chartXVisibleDomain limits visible range.
import Charts

@available(iOS 26, *)
struct WorkoutVolumeChart: View {
    let history: [WorkoutEntry]

    var body: some View {
        Chart(history) { entry in
            BarMark(
                x: .value("Date", entry.date),
                y: .value("Volume", entry.totalVolume)
            )
            .foregroundStyle(.blue.gradient)
        }
        .chartScrollableAxes(.horizontal)
        .chartXVisibleDomain(length: 7) // Show 7 days at a time
        .frame(height: 200)
    }
}

struct WorkoutEntry: Identifiable {
    let id: String
    let date: Date
    let totalVolume: Double
}
