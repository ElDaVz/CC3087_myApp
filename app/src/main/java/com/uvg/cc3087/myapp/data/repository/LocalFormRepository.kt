package com.uvg.cc3087.myapp.data.repository

import android.content.Context
import com.uvg.cc3087.myapp.data.FormEditorSampleData
import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.data.local.AppPreferencesManager
import com.uvg.cc3087.myapp.data.local.FormDatabaseHelper
import com.uvg.cc3087.myapp.data.local.FullFormDraft
import com.uvg.cc3087.myapp.data.model.FormSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LocalFormRepository(context: Context) : FormRepository {

    private val dbHelper = FormDatabaseHelper(context.applicationContext)
    private val prefsManager = AppPreferencesManager(context.applicationContext)
    private val _formsFlow = MutableStateFlow<List<FormSummary>>(emptyList())

    init {
        seedDatabaseIfEmpty()
        refreshForms()
    }

    private fun seedDatabaseIfEmpty() {
        if (!dbHelper.hasForms()) {
            FormSampleData.forms.forEach { summary ->
                val draft = FullFormDraft(
                    id = summary.id,
                    title = summary.title,
                    status = summary.status,
                    ownerId = "guest_user",
                    fields = FormEditorSampleData.templateFields,
                    responseCount = summary.responseCount,
                    updatedDate = summary.updatedDate,
                    imageUrl = summary.imageUrl
                )
                dbHelper.insertOrUpdateForm(draft)
            }
        }
    }

    private fun refreshForms() {
        _formsFlow.value = dbHelper.getAllForms()
    }

    override fun observeForms(): Flow<List<FormSummary>> = _formsFlow.asStateFlow()

    override suspend fun getForm(formId: String): FullFormDraft? = withContext(Dispatchers.IO) {
        dbHelper.getForm(formId)
    }

    override suspend fun saveForm(draft: FullFormDraft) = withContext(Dispatchers.IO) {
        dbHelper.insertOrUpdateForm(draft)
        refreshForms()
    }

    override suspend fun deleteForm(formId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteForm(formId)
        refreshForms()
    }

    override fun observeLastEditedFormId(): Flow<String?> = prefsManager.lastEditedFormId

    override suspend fun setLastEditedFormId(formId: String?) = withContext(Dispatchers.IO) {
        prefsManager.saveLastEditedFormId(formId)
    }
}
