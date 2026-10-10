package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.data.model.TemplateType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SampleTemplateRepository : TemplateRepository {
    override fun observeTemplates(): Flow<List<FormTemplate>> =
        flowOf(TemplateSampleData.templates)

    override fun getInitialFields(templateType: TemplateType): List<FormFieldDraft> =
        // conservamos los mismos ejemplos que ya abrían las cuatro plantillas
        when (templateType) {
            TemplateType.JOB_APPLICATION,
            TemplateType.ORDER_FORM,
            TemplateType.EVENT_RSVP,
            TemplateType.FEEDBACK -> FormEditorSampleData.templateFields.toList()
        }
}
