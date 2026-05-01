import shared

// MARK: - Shared KoinHelper singleton
//
// All ViewModel wrappers must use `KoinBridge.shared` instead of
// instantiating `KoinHelper()` directly.  Creating multiple `KoinComponent`
// instances is unnecessary and keeps Koin's DI graph alive longer than needed.

enum KoinBridge {
    // KoinHelper is a read-only DI accessor after initialization. It is
    // created once at app launch and never mutated; nonisolated(unsafe) is
    // correct here — there is no concurrent mutation risk.
    nonisolated(unsafe) static let shared = KoinHelper()
}
