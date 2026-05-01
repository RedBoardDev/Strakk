import Foundation
import os
@preconcurrency import shared

/// KMP objects cross the Kotlin/Swift bridge as opaque reference types whose thread safety
/// the Kotlin runtime guarantees. This wrapper lets them cross Swift concurrency boundaries.
private struct KMPBridged<T>: @unchecked Sendable { let value: T }

/// Collects a KMP StateFlow into an AsyncStream, bridging Kotlin coroutines to Swift concurrency.
func observeFlow<T: AnyObject>(
    _ flow: Kotlinx_coroutines_coreStateFlow
) -> AsyncStream<T> {
    AsyncStream { continuation in
        let job = FlowCollectorJob(flow: flow) { value in
            if let typed = value as? T {
                continuation.yield(KMPBridged(value: typed).value)
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
                continuation.yield(KMPBridged(value: typed).value)
            }
        }
        continuation.onTermination = { _ in
            job.cancel()
        }
        job.start()
    }
}

// MARK: - Internal

/// Bridges a KMP Flow to a Swift Task, using OSAllocatedUnfairLock for thread-safe task storage.
private final class FlowCollectorJob: NSObject, Sendable {
    private let flow: Kotlinx_coroutines_coreFlow
    private let onEmit: @Sendable (Any?) -> Void
    private let taskState = OSAllocatedUnfairLock<Task<Void, Never>?>(initialState: nil)

    init(flow: Kotlinx_coroutines_coreFlow, onEmit: @escaping @Sendable (Any?) -> Void) {
        self.flow = flow
        self.onEmit = onEmit
    }

    func start() {
        let collector = BlockCollector(onEmit: onEmit)
        taskState.withLock { state in
            state = Task {
                do {
                    try await flow.collect(collector: collector)
                } catch {
                    // Flow completed or cancelled
                }
            }
        }
    }

    func cancel() {
        taskState.withLock { $0?.cancel() }
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
