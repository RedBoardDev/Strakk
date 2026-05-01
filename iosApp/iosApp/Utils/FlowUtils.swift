import Foundation
@preconcurrency import shared

/// Collects a KMP StateFlow into an AsyncStream, bridging Kotlin coroutines to Swift concurrency.
/// KMP objects are reference types managed by the Kotlin runtime; thread safety is the KMP layer's responsibility.
func observeFlow<T: AnyObject>(
    _ flow: Kotlinx_coroutines_coreStateFlow
) -> AsyncStream<T> {
    AsyncStream { continuation in
        let job = FlowCollectorJob(flow: flow) { value in
            if let typed = value as? T {
                // KMP objects cross the Kotlin/Swift boundary as opaque reference types.
                // Their thread safety is the Kotlin runtime's responsibility at this bridge point.
                nonisolated(unsafe) let bridged = typed
                continuation.yield(bridged)
            }
        }
        continuation.onTermination = { _ in
            job.cancel()
        }
        job.start()
    }
}

/// Collects a KMP Flow (e.g. Channel-based effects) into an AsyncStream.
func observeFlow<T: AnyObject>(
    _ flow: Kotlinx_coroutines_coreFlow
) -> AsyncStream<T> {
    AsyncStream { continuation in
        let job = FlowCollectorJob(flow: flow) { value in
            if let typed = value as? T {
                // KMP objects cross the Kotlin/Swift boundary as opaque reference types.
                // Their thread safety is the Kotlin runtime's responsibility at this bridge point.
                nonisolated(unsafe) let bridged = typed
                continuation.yield(bridged)
            }
        }
        continuation.onTermination = { _ in
            job.cancel()
        }
        job.start()
    }
}

// MARK: - Internal

/// Bridges a KMP Flow to a Swift Task.
///
/// `task` is written exactly once (in `start()`) and then only cancelled (in `cancel()`).
/// Both call sites are always the same AsyncStream continuation context, so there is no
/// concurrent mutation. `nonisolated(unsafe)` on the stored task is therefore safe and
/// correct — it avoids `@unchecked Sendable` on the whole class.
private final class FlowCollectorJob: NSObject, Sendable {
    private let flow: Kotlinx_coroutines_coreFlow
    private let onEmit: @Sendable (Any?) -> Void
    nonisolated(unsafe) private var task: Task<Void, Never>?

    init(flow: Kotlinx_coroutines_coreFlow, onEmit: @escaping @Sendable (Any?) -> Void) {
        self.flow = flow
        self.onEmit = onEmit
    }

    func start() {
        let collector = BlockCollector(onEmit: onEmit)
        task = Task {
            do {
                try await flow.collect(collector: collector)
            } catch {
                // Flow completed or cancelled
            }
        }
    }

    func cancel() {
        task?.cancel()
    }
}

private final class BlockCollector: NSObject, Sendable, Kotlinx_coroutines_coreFlowCollector {
    private let onEmit: @Sendable (Any?) -> Void

    init(onEmit: @escaping @Sendable (Any?) -> Void) {
        self.onEmit = onEmit
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        onEmit(value)
        completionHandler(nil)
    }
}
