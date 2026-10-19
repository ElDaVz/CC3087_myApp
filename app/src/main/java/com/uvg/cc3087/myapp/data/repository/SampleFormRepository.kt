package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.data.model.FormSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// seguimos usando los mismos ejemplos mientras se integra el guardado local
class SampleFormRepository : FormRepository {
    private val forms = MutableStateFlow(FormSampleData.forms)

    override fun observeForms(): Flow<List<FormSummary>> = forms.asStateFlow()

    fun updateSampleForms(localizedForms: List<FormSummary>) {
        forms.value = localizedForms.toList()
    }
}
