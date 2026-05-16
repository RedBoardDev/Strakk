package com.strakk.shared.di

import com.strakk.shared.data.pdf.IosHtmlToPdfRenderer
import com.strakk.shared.domain.service.HtmlToPdfRenderer
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal actual fun platformDataModule(): Module = module {
    singleOf(::IosHtmlToPdfRenderer) { bind<HtmlToPdfRenderer>() }
}
