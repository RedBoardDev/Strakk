package com.strakk.shared.domain.service

import com.strakk.shared.domain.model.PdfExportOptions

interface CheckInPdfGenerator {
    suspend fun generate(checkInId: String, options: PdfExportOptions = PdfExportOptions()): ByteArray
}
