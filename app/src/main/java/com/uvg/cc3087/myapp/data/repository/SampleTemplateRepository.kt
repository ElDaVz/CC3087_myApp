package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.TemplateSampleData
import com.uvg.cc3087.myapp.data.model.FormTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SampleTemplateRepository : TemplateRepository {
    override fun observeTemplates(): Flow<List<FormTemplate>> =
        flowOf(TemplateSampleData.templates)
}
