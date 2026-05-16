package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.model.PdfExportOptions
import com.strakk.shared.domain.repository.PdfPreferencesRepository

class GetPdfExportPreferencesUseCase(
    private val repository: PdfPreferencesRepository,
) {
    operator fun invoke(): PdfExportOptions = repository.getExportOptions()
}
