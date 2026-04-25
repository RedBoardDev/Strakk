import Foundation
import shared

/// Collects a KMP StateFlow into an AsyncStream, bridging Kotlin coroutines to Swift concurrency.
/// KMP objects are reference types managed by the Kotlin runtime; thread safety is the KMP layer's responsibility.
func observeFlow<T: AnyObject>(
    _ flow: Kotlinx_coroutines_coreStateFlow
) -> AsyncStream<T> {
    AsyncStream { continuation in
        let job = FlowCollectorJob(flow: flow) { value in
            if let typed = value as? T {
                // KMP objects are reference types whose thread-safety is managed by the Kotlin runtime.
                // nonisolated(unsafe) suppresses the Swift 6 sendability diagnostic at this bridge point.
                nonisolated(unsafe) let sendable = typed
                continuation.yield(sendable)
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
                // KMP objects are reference types whose thread-safety is managed by the Kotlin runtime.
                // nonisolated(unsafe) suppresses the Swift 6 sendability diagnostic at this bridge point.
                nonisolated(unsafe) let sendable = typed
                continuation.yield(sendable)
            }
        }
        continuation.onTermination = { _ in
            job.cancel()
        }
        job.start()
    }
}

// MARK: - Internal

private final class FlowCollectorJob: NSObject, @unchecked Sendable {
    private let flow: Kotlinx_coroutines_coreFlow
    private let onEmit: (Any?) -> Void
    private var task: Task<Void, Never>?

    init(flow: Kotlinx_coroutines_coreFlow, onEmit: @escaping (Any?) -> Void) {
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

private final class BlockCollector: NSObject, @unchecked Sendable, Kotlinx_coroutines_coreFlowCollector {
    private let onEmit: (Any?) -> Void

    init(onEmit: @escaping (Any?) -> Void) {
        self.onEmit = onEmit
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        onEmit(value)
        completionHandler(nil)
    }
}
