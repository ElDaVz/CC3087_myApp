package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.FormSampleData
import com.uvg.cc3087.myapp.data.model.FormSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// seguimos usando los mismos ejemplos mientras se integra el guardado local
class SampleFormRepository : FormRepository {
    override fun observeForms(): Flow<List<FormSummary>> = flowOf(FormSampleData.forms)
}
