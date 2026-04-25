import Observation

@MainActor
@Observable
final class AppNavigator {
    static let shared = AppNavigator()

    var selectedTab: Int = 0

    private init() {}

    func navigateToToday() {
        selectedTab = 0
    }
}
