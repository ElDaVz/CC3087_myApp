package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.data.model.TemplateType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SampleTemplateRepository : TemplateRepository {
    private var initialFields = FormEditorSampleData.templateFields

    fun updateSampleFields(localizedFields: List<FormFieldDraft>) {
        initialFields = localizedFields.map { it.copy(options = it.options.toList()) }
    }

    override fun observeTemplates(): Flow<List<FormTemplate>> =
        flowOf(TemplateSampleData.templates)

    override fun getInitialFields(templateType: TemplateType): List<FormFieldDraft> =
        // conservamos los mismos ejemplos que ya abrían las cuatro plantillas
        when (templateType) {
            TemplateType.JOB_APPLICATION,
            TemplateType.ORDER_FORM,
            TemplateType.EVENT_RSVP,
            TemplateType.FEEDBACK -> initialFields.map { it.copy(options = it.options.toList()) }
        }
}
