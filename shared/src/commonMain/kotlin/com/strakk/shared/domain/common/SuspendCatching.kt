package com.strakk.shared.domain.common

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching] but rethrows [CancellationException] so coroutine cancellation
 * works correctly. Use this in suspend functions instead of [runCatching].
 *
 * Why: `runCatching` catches `CancellationException`, which breaks structured concurrency
 * — cancelled coroutines appear as failed `Result` values and propagate as user-facing errors.
 */
internal inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (t: Throwable) {
        Result.failure(t)
    }
