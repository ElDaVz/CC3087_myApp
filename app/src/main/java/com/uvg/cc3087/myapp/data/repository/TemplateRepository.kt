package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormTemplate
import com.uvg.cc3087.myapp.data.model.TemplateType
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun observeTemplates(): Flow<List<FormTemplate>>

    // los campos del catálogo local también salen por el repositorio, no por la pantalla
    fun getInitialFields(templateType: TemplateType): List<FormFieldDraft>
}
