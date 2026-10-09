package com.uvg.cc3087.myapp.di

import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.SampleFormRepository
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository

// aquí elegimos la fuente de datos sin hacer que las pantallas la construyan
object AppContainer {
    val formRepository: FormRepository by lazy { SampleFormRepository() }
    val templateRepository: TemplateRepository by lazy { SampleTemplateRepository() }
}
