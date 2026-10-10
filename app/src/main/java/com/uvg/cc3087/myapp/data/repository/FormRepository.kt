package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormSummary
import kotlinx.coroutines.flow.Flow

// empezamos con la lectura que también utiliza el repositorio de borradores del equipo
interface FormRepository {
    fun observeForms(): Flow<List<FormSummary>>
}
