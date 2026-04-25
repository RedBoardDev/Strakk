import SwiftUI
import shared

@main
struct iOSApp: App {
    @State private var authWrapper: RootViewModelWrapper

    init() {
        KoinHelperKt.doInitKoin()
        _authWrapper = State(initialValue: RootViewModelWrapper())
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(authWrapper)
        }
    }
}
