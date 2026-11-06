package com.uvg.cc3087.myapp.di

import android.content.Context
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.data.model.FormFieldDraft

// aquí elegimos la fuente de datos sin hacer que las pantallas la construyan
object AppContainer {
    private lateinit var appContext: Context
    private val database by lazy {
        com.uvg.cc3087.myapp.data.security.CoverEncryptionMigration.migrate(appContext)
        // Remove the old Firebase Auth SDK store before enabling the encrypted REST session.
        java.io.File(appContext.applicationInfo.dataDir, "shared_prefs").listFiles()?.filter {
            it.name.startsWith("com.google.firebase.auth.")
        }?.forEach { appContext.deleteSharedPreferences(it.name.removeSuffix(".bak").removeSuffix(".xml")) }
        FormLinkDatabase.open(appContext)
    }
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
