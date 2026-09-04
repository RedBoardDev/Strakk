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
import platform.Foundation.NSURL
import platform.Foundation.NSSelectorFromString
import platform.PDFKit.PDFAnnotation
import platform.PDFKit.PDFAnnotationSubtypeLink
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.PDFKit.PDFSelection
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
        val pdfLinks = extractPdfLinks(html)
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

                    val annotatedPdfData = addLinkAnnotations(pdfData, pdfLinks)
                    continuation.resume(annotatedPdfData.toByteArray())
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

    private fun extractPdfLinks(html: String): List<PdfLink> = PDF_LINK_REGEX
        .findAll(html)
        .map { match ->
            PdfLink(
                url = match.groupValues[1],
                text = match.groupValues[2],
            )
        }
        .toList()

    /**
     * UIPrintPageRenderer preserves the link text but drops HTML link annotations.
     * Recreate them on the rendered PDF so Preview and iOS can open the workouts.
     */
    private fun addLinkAnnotations(pdfData: NSData, links: List<PdfLink>): NSData {
        if (links.isEmpty()) return pdfData

        val document = PDFDocument(data = pdfData)
        val selectionsByText = links
            .map { it.text }
            .distinct()
            .associateWith { text ->
                document.findString(text, withOptions = 0u)
                    .filterIsInstance<PDFSelection>()
                    .iterator()
            }

        links.forEach { link ->
            val selections = selectionsByText[link.text] ?: return@forEach
            if (!selections.hasNext()) return@forEach

            val selection = selections.next()
            val page = selection.pages.firstOrNull() as? PDFPage ?: return@forEach
            val annotation = PDFAnnotation(
                bounds = selection.boundsForPage(page),
                forType = PDFAnnotationSubtypeLink,
                withProperties = null,
            )
            annotation.performSelector(
                NSSelectorFromString("setURL:"),
                withObject = NSURL(string = link.url),
            )
            page.addAnnotation(annotation)
        }

        return document.dataRepresentation() ?: pdfData
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

private data class PdfLink(
    val url: String,
    val text: String,
)

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
private val PDF_LINK_REGEX = Regex(
    """<a\b[^>]*href="(https://hevy\.com/workout/[A-Za-z0-9-]+)"[^>]*data-pdf-link-text="([^"]+)"[^>]*>""",
)
