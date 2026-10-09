package com.uvg.cc3087.myapp.data.model

data class FormTemplate(
    val id: String,
    val type: TemplateType
)

// el tipo identifica la plantilla sin depender de su texto en español o inglés
enum class TemplateType {
    JOB_APPLICATION,
    ORDER_FORM,
    EVENT_RSVP,
    FEEDBACK
}
