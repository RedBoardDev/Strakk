import Observation

@MainActor
@Observable
final class AppNavigator {
    var selectedTab: Int = 0

    init() {}

    func navigateToToday() {
        selectedTab = 0
    }
}
