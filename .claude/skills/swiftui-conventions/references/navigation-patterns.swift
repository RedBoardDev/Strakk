import SwiftUI
import Shared // KMP shared module

// =============================================================================
// Pattern 1: NavigationStack with typed destinations
// =============================================================================

/// Route enum — type-safe navigation destinations.
///
/// Use an enum (NOT string paths) for compile-time safety.
enum AppRoute: Hashable {
    case sessionList
    case sessionDetail(sessionId: String)
    case createSession
    case mealLog
    case profile
}

/// Router — owns the navigation path.
@MainActor
@Observable
final class AppRouter {
    var path = NavigationPath()

    func navigate(to route: AppRoute) {
        path.append(route)
    }

    func pop() {
        guard !path.isEmpty else { return }
        path.removeLast()
    }

    func popToRoot() {
        path.removeLast(path.count)
    }
}

/// Root navigation view with typed destination handling.
struct RootNavigationView: View {
    @State private var router = AppRouter()

    var body: some View {
        NavigationStack(path: $router.path) {
            SessionListScreen()
                .navigationDestination(for: AppRoute.self) { route in
                    switch route {
                    case .sessionList:
                        SessionListScreen()
                    case .sessionDetail(let sessionId):
                        SessionDetailScreen(sessionId: sessionId)
                    case .createSession:
                        CreateSessionScreen()
                    case .mealLog:
                        MealLogScreen()
                    case .profile:
                        ProfileScreen()
                    }
                }
        }
        .environment(router)
    }
}

// =============================================================================
// Pattern 2: Tab bar setup
// =============================================================================

enum AppTab: String, CaseIterable {
    case home
    case sessions
    case nutrition
    case profile

    var title: String {
        switch self {
        case .home: "Home"
        case .sessions: "Sessions"
        case .nutrition: "Nutrition"
        case .profile: "Profile"
        }
    }

    var systemImage: String {
        switch self {
        case .home: "house"
        case .sessions: "figure.strengthtraining.traditional"
        case .nutrition: "fork.knife"
        case .profile: "person.circle"
        }
    }
}

struct MainTabView: View {
    @State private var selectedTab: AppTab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(AppTab.allCases, id: \.self) { tab in
                Tab(tab.title, systemImage: tab.systemImage, value: tab) {
                    NavigationStack {
                        tabContent(for: tab)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func tabContent(for tab: AppTab) -> some View {
        switch tab {
        case .home:
            HomeScreen()
        case .sessions:
            SessionListScreen()
        case .nutrition:
            NutritionScreen()
        case .profile:
            ProfileScreen()
        }
    }
}

// =============================================================================
// Pattern 3: Sheet presentation (data-driven)
// =============================================================================

/// PREFERRED: data-driven sheets using `.sheet(item:)`.
///
/// The sheet is presented when the item is non-nil and dismissed when set to nil.
/// This avoids boolean state synchronization issues.
struct SessionListScreen: View {
    @State private var selectedSession: Session?
    @State private var showCreateSession = false

    var body: some View {
        List {
            // ... session rows ...
        }
        .navigationTitle("Sessions")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("New", systemImage: "plus") {
                    showCreateSession = true
                }
            }
        }
        // PREFERRED: item-driven sheet
        .sheet(item: $selectedSession) { session in
            SessionDetailSheet(session: session)
        }
        // OK for creation flows where there's no item yet
        .sheet(isPresented: $showCreateSession) {
            CreateSessionScreen()
        }
    }
}

// =============================================================================
// Pattern 4: Deep linking for magic link auth
// =============================================================================

/// Handles Supabase magic link deep links.
///
/// The magic link URL contains an auth token that needs to be
/// extracted and passed to the Supabase auth handler.
@main
struct StrakkApp: App {
    @State private var router = AppRouter()
    @State private var authManager: AuthManager

    init() {
        // Initialize Koin
        InitKoinKt.doInitKoin(config: nil)
        _authManager = State(initialValue: AuthManager())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(router)
                .environment(authManager)
                .onOpenURL { url in
                    handleDeepLink(url)
                }
        }
    }

    private func handleDeepLink(_ url: URL) {
        // Supabase magic link format:
        // strakk://auth/callback#access_token=...&refresh_token=...
        guard url.scheme == "strakk",
              url.host == "auth" else {
            return
        }

        Task {
            await authManager.handleMagicLinkCallback(url: url)
        }
    }
}

/// Auth state manager — bridges KMP auth with SwiftUI.
@MainActor
@Observable
final class AuthManager {
    enum AuthState {
        case loading
        case authenticated
        case unauthenticated
    }

    var authState: AuthState = .loading

    func handleMagicLinkCallback(url: URL) async {
        do {
            // Call KMP shared auth handler
            try await SupabaseAuthKt.handleDeepLink(url: url.absoluteString)
            authState = .authenticated
        } catch {
            authState = .unauthenticated
        }
    }
}

/// Root content that switches between auth and main app.
struct ContentView: View {
    @Environment(AuthManager.self) private var authManager

    var body: some View {
        switch authManager.authState {
        case .loading:
            ProgressView("Loading...")
        case .authenticated:
            MainTabView()
        case .unauthenticated:
            LoginScreen()
        }
    }
}

// =============================================================================
// Placeholder views (for compilation)
// =============================================================================

struct SessionDetailScreen: View {
    let sessionId: String
    var body: some View { Text("Session \(sessionId)") }
}

struct CreateSessionScreen: View {
    var body: some View { Text("Create Session") }
}

struct MealLogScreen: View {
    var body: some View { Text("Meal Log") }
}

struct ProfileScreen: View {
    var body: some View { Text("Profile") }
}

struct HomeScreen: View {
    var body: some View { Text("Home") }
}

struct NutritionScreen: View {
    var body: some View { Text("Nutrition") }
}

struct LoginScreen: View {
    var body: some View { Text("Login") }
}

struct SessionDetailSheet: View {
    let session: Session
    var body: some View { Text("Detail: \(session.name)") }
}
