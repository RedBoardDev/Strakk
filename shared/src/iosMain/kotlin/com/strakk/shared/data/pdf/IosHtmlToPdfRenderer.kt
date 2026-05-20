package com.strakk.shared.data.pdf

import com.strakk.shared.domain.service.HtmlToPdfRenderer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIGraphicsBeginPDFContextToData
import platform.UIKit.UIGraphicsBeginPDFPageWithInfo
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIPrintFormatter
import platform.UIKit.UIPrintPageRenderer
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Renders HTML to a real multi-page A4 PDF.
 *
 * Approach: WKWebView loads the HTML (resolves data: URI images, runs JavaScript,
 * applies CSS), then we call viewPrintFormatter() via performSelector (UIKit
 * category method on UIView, not directly reachable from the WebKit Kotlin binding)
 * and feed the result to UIPrintPageRenderer + UIGraphicsPDF for proper A4 pagination.
 *
 * This is the standard iOS pattern for HTML → PDF with images.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosHtmlToPdfRenderer : HtmlToPdfRenderer {

    override suspend fun render(html: String): ByteArray = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val config = WKWebViewConfiguration()
            val webView = WKWebView(
                frame = CGRectMake(0.0, 0.0, A4_WIDTH_PT, A4_HEIGHT_PT),
                configuration = config,
            )

            val delegate = object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                    // viewPrintFormatter() is a UIKit category on UIView; not available
                    // in the WebKit cinterop binding, so call it via performSelector.
                    val viewPrintFormatterSel = NSSelectorFromString("viewPrintFormatter")
                    @Suppress("UNCHECKED_CAST")
                    val printFormatter = webView.performSelector(viewPrintFormatterSel)
                        as? UIPrintFormatter

                    if (printFormatter == null) {
                        continuation.resumeWithException(
                            RuntimeException("viewPrintFormatter returned null")
                        )
                        return
                    }

                    val renderer = A4PrintPageRenderer().apply {
                        addPrintFormatter(printFormatter, startingAtPageAtIndex = 0L)
                    }

                    val paperRect = CGRectMake(0.0, 0.0, A4_WIDTH_PT, A4_HEIGHT_PT)
                    val pdfData = NSMutableData()
                    UIGraphicsBeginPDFContextToData(pdfData, paperRect, null)
                    val numPages = renderer.numberOfPages.toInt()
                    for (i in 0 until numPages) {
                        UIGraphicsBeginPDFPageWithInfo(paperRect, null)
                        renderer.drawPageAtIndex(i.toLong(), inRect = paperRect)
                    }
                    UIGraphicsEndPDFContext()

                    continuation.resume(pdfData.toByteArray())
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
            continuation.invokeOnCancellation { webView.stopLoading() }
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

@OptIn(ExperimentalForeignApi::class)
private class A4PrintPageRenderer : UIPrintPageRenderer() {
    // printableRect has top/bottom hardware margins applied to every page.
    // Width is full A4 (no horizontal scaling) — CSS .page{padding} handles sides.
    override fun paperRect(): CValue<CGRect> = CGRectMake(0.0, 0.0, A4_WIDTH_PT, A4_HEIGHT_PT)
    override fun printableRect(): CValue<CGRect> = CGRectMake(
        x = 0.0,
        y = V_MARGIN_PT,
        width = A4_WIDTH_PT,
        height = A4_HEIGHT_PT - 2 * V_MARGIN_PT,
    )
}

// A4 at 72 DPI: 8.27" × 11.69" → 595 × 842 pt.
private const val A4_WIDTH_PT = 595.0
private const val A4_HEIGHT_PT = 842.0
private const val V_MARGIN_PT = 40.0 // ≈14 mm — top + bottom margin on every page
