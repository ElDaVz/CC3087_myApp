package com.uvg.cc3087.myapp.testing

import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.data.model.TemplateType
import com.uvg.cc3087.myapp.data.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// este repositorio vive solo en las pruebas y deja controlar los datos de cada plantilla
class FakeTemplateRepository(
    initialTemplates: List<FormTemplate> = emptyList(),
    private val initialFieldsByTemplate: Map<TemplateType, List<FormFieldDraft>> = emptyMap()
) : TemplateRepository {
    val templates = MutableStateFlow(initialTemplates)
    val requestedTemplateTypes = mutableListOf<TemplateType>()

    override fun observeTemplates(): Flow<List<FormTemplate>> = templates

    override fun getInitialFields(templateType: TemplateType): List<FormFieldDraft> {
        requestedTemplateTypes += templateType
        return initialFieldsByTemplate[templateType].orEmpty()
    }
}
