package com.uvg.cc3087.myapp.di

import android.content.Context
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.RoomFormRepository
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.data.model.FormFieldDraft

// aquí elegimos la fuente de datos sin hacer que las pantallas la construyan
object AppContainer {
    private var localForms: FormRepository? = null
    private val sampleTemplates by lazy { SampleTemplateRepository() }
    @Synchronized
    fun formRepository(context: Context): FormRepository {
        return localForms ?: RoomFormRepository(FormLinkDatabase.open(context).drafts()).also {
            localForms = it
        }
    }
    val templateRepository: TemplateRepository get() = sampleTemplates

    // solo los ejemplos reciben traducciones desde la raíz, no los viewmodels
    fun updateTemplateFields(fields: List<FormFieldDraft>) {
        sampleTemplates.updateSampleFields(fields)
    }
}
