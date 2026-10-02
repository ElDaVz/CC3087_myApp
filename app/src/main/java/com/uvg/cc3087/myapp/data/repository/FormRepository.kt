package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.local.FullFormDraft
import com.uvg.cc3087.myapp.data.model.FormSummary
import kotlinx.coroutines.flow.Flow

interface FormRepository {
    fun observeForms(): Flow<List<FormSummary>>
    suspend fun getForm(formId: String): FullFormDraft?
    suspend fun saveForm(draft: FullFormDraft)
    suspend fun deleteForm(formId: String)
    fun observeLastEditedFormId(): Flow<String?>
    suspend fun setLastEditedFormId(formId: String?)
}
