package com.strakk.shared.data.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.webkit.WebView
import android.webkit.WebViewClient
import com.strakk.shared.domain.service.HtmlToPdfRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AndroidHtmlToPdfRenderer(
    private val context: Context,
) : HtmlToPdfRenderer {

    override suspend fun render(html: String): ByteArray = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    try {
                        val width = (595 * view.resources.displayMetrics.density).toInt()
                        val height = (842 * view.resources.displayMetrics.density).toInt()
                        view.measure(
                            android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
                        )
                        view.layout(0, 0, width, view.measuredHeight)

                        val doc = PdfDocument()
                        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                        val page = doc.startPage(pageInfo)
                        val scale = 595f / width
                        page.canvas.scale(scale, scale)
                        view.draw(page.canvas)
                        doc.finishPage(page)

                        val out = ByteArrayOutputStream()
                        doc.writeTo(out)
                        doc.close()
                        webView.destroy()
                        continuation.resume(out.toByteArray())
                    } catch (e: Exception) {
                        webView.destroy()
                        continuation.resumeWithException(
                            RuntimeException("PDF generation failed: ${e.message}", e),
                        )
                    }
                }
            }

            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }
}
