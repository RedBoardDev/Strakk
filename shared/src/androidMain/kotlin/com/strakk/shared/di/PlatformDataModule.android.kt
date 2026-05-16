package com.strakk.shared.di

import com.strakk.shared.data.pdf.AndroidHtmlToPdfRenderer
import com.strakk.shared.domain.service.HtmlToPdfRenderer
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformDataModule(): Module = module {
    single<HtmlToPdfRenderer> { AndroidHtmlToPdfRenderer(context = get()) }
}
