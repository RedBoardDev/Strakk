package com.strakk.shared.domain.repository

import com.strakk.shared.domain.model.PdfExportOptions

interface PdfPreferencesRepository {
    fun getExportOptions(): PdfExportOptions
    fun saveExportOptions(options: PdfExportOptions)
}
