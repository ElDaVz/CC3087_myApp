package com.uvg.cc3087.myapp.data.model

enum class FormFieldType(
    val label: String,
    val defaultTitle: String
) {
    TEXT("Texto", "Pregunta de texto"),
    MULTIPLE_CHOICE("Opción múltiple", "Selecciona una opción"),
    DATE("Fecha", "Selecciona una fecha")
}

data class FormFieldDraft(
    val id: String,
    val type: FormFieldType,
    val title: String,
    val required: Boolean = false
)
