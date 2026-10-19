package com.uvg.cc3087.myapp.di

import com.uvg.cc3087.myapp.data.repository.FormRepository
import com.uvg.cc3087.myapp.data.repository.SampleFormRepository
import com.uvg.cc3087.myapp.data.repository.SampleTemplateRepository
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormSummary

// aquí elegimos la fuente de datos sin hacer que las pantallas la construyan
object AppContainer {
    private val sampleForms by lazy { SampleFormRepository() }
    private val sampleTemplates by lazy { SampleTemplateRepository() }
    val formRepository: FormRepository get() = sampleForms
    val templateRepository: TemplateRepository get() = sampleTemplates

    // solo los ejemplos reciben traducciones desde la raíz, no los viewmodels
    fun updateSampleData(forms: List<FormSummary>, fields: List<FormFieldDraft>) {
        sampleForms.updateSampleForms(forms)
        sampleTemplates.updateSampleFields(fields)
    }
}
