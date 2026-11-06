package com.uvg.cc3087.myapp.di

import android.content.Context
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.data.model.FormFieldDraft

// aquí elegimos la fuente de datos sin hacer que las pantallas la construyan
object AppContainer {
    private lateinit var appContext: Context
    private val database by lazy { FormLinkDatabase.open(appContext) }
    val coverStorage by lazy { com.uvg.cc3087.myapp.data.local.PrivateCoverStorage(appContext) }
    private val sampleTemplates by lazy { SampleTemplateRepository() }
    @Synchronized
    fun initialize(context: Context) {
        if (!::appContext.isInitialized) appContext = context.applicationContext
    }
    val backend by lazy { BackendServices(appContext, database, coverStorage) }
    val templateRepository: TemplateRepository get() = sampleTemplates
    val requestRepository by lazy { com.uvg.cc3087.myapp.data.repository.RoomRequestRepository(database) }
    val requestsAreDemo get() = false

    // solo los ejemplos reciben traducciones desde la raíz, no los viewmodels
    fun updateTemplateFields(fields: List<FormFieldDraft>) {
        sampleTemplates.updateSampleFields(fields)
    }
}
