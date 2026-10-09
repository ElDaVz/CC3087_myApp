package com.uvg.cc3087.myapp.di

import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.SampleFormRepository

// aquí elegimos la fuente de datos sin hacer que las pantallas la construyan
object AppContainer {
    val formRepository: FormRepository by lazy { SampleFormRepository() }
}
