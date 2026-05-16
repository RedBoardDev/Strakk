package com.strakk.shared.domain.service

/**
 * Renders an HTML string to a PDF byte array.
 *
 * Platform-specific implementations:
 * - iOS: WKWebView + createPDF(configuration:)
 * - Android: WebView + PrintDocumentAdapter
 *
 * Implementations are provided per-platform and injected via Koin.
 */
interface HtmlToPdfRenderer {
    suspend fun render(html: String): ByteArray
}
