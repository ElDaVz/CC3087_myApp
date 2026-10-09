package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormTemplate
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun observeTemplates(): Flow<List<FormTemplate>>
}
