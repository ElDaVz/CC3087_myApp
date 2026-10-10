package com.uvg.cc3087.myapp.data.model

// el modelo identifica el tipo y la ui se encarga de cómo nombrarlo
enum class FormFieldType {
    TEXT,
    MULTIPLE_CHOICE,
    DATE
}

data class FormFieldDraft(
    val id: String,
    val type: FormFieldType,
    val title: String,
    val required: Boolean = false
)
