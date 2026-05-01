import SwiftUI
import shared

@main
struct iOSApp: App {
    @State private var authWrapper: RootViewModelWrapper
    @State private var navigator = AppNavigator()

    init() {
        URLCache.shared = URLCache(
            memoryCapacity: 50 * 1024 * 1024,   // 50 MB memory
            diskCapacity: 200 * 1024 * 1024      // 200 MB disk
        )
        KoinHelperKt.doInitKoin()
        _authWrapper = State(initialValue: RootViewModelWrapper())
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(authWrapper)
                .environment(navigator)
        }
    }
}
