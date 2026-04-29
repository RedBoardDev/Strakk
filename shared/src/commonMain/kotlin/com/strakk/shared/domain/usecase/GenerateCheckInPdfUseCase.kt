package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.PdfExportOptions
import com.strakk.shared.domain.service.CheckInPdfGenerator

class GenerateCheckInPdfUseCase(
    private val generator: CheckInPdfGenerator,
) {
    suspend operator fun invoke(checkInId: String, options: PdfExportOptions = PdfExportOptions()): ByteArray =
        generator.generate(checkInId, options)
}
