package com.uvg.cc3087.myapp.data.model

// estos estados nos permiten filtrar formularios sin depender del texto de la interfaz
enum class FormStatus {
    ACTIVE,
    DRAFT
}

// cada formulario conserva un id único para usarlo como key en la lazy column
data class FormSummary(
    val id: String,
    val title: String,
    val responseCount: Int,
    val updatedDate: String,
    val status: FormStatus,
    val imageUrl: String
)
