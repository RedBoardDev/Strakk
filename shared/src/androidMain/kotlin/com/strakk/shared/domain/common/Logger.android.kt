package com.strakk.shared.domain.common

import android.util.Log

actual fun createLogger(): Logger = AndroidLogger

private object AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
