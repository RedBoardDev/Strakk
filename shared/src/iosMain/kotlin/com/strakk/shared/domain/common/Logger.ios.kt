package com.strakk.shared.domain.common

import platform.Foundation.NSLog

actual fun createLogger(): Logger = IosLogger

private object IosLogger : Logger {
    override fun d(tag: String, message: String) {
        NSLog("[D][%s] %s", tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        val detail = throwable?.stackTraceToString()?.let { "\n$it" } ?: ""
        NSLog("[E][%s] %s%s", tag, message, detail)
    }
}
