package com.strakk.shared.data.pdf

import com.strakk.shared.domain.service.HtmlToPdfRenderer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
internal class IosHtmlToPdfRenderer : HtmlToPdfRenderer {

    override suspend fun render(html: String): ByteArray = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val config = WKWebViewConfiguration()
            val webView = WKWebView(frame = CGRectMake(0.0, 0.0, 595.0, 842.0), configuration = config)

            val delegate = object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                    webView.createPDFWithConfiguration(null) { data, error ->
                        if (error != null || data == null) {
                            continuation.resumeWithException(
                                RuntimeException("PDF generation failed: ${error?.localizedDescription}")
                            )
                        } else {
                            continuation.resume(data.toByteArray())
                        }
                    }
                }

                override fun webView(
                    webView: WKWebView,
                    didFailNavigation: WKNavigation?,
                    withError: platform.Foundation.NSError,
                ) {
                    continuation.resumeWithException(
                        RuntimeException("WebView navigation failed: ${withError.localizedDescription}")
                    )
                }
            }

            webView.navigationDelegate = delegate
            webView.loadHTMLString(html, baseURL = null)
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val bytes = ByteArray(size)
        if (size > 0) {
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), this@toByteArray.bytes, length)
            }
        }
        return bytes
    }
}
