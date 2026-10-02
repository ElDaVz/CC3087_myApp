package com.uvg.cc3087.myapp.data.model

enum class FormFieldType(
    val label: String,
    val defaultTitle: String
) {
    TEXT("Texto corto", "Pregunta de texto"),
    PARAGRAPH("Párrafo", "Respuesta larga"),
    NUMBER("Número", "Cantidad"),
    EMAIL("Correo electrónico", "Correo electrónico"),
    CHECKBOX("Casilla", "Acepto"),
    CHECKBOX_GROUP("Casillas múltiples", "Selecciona las opciones"),
    DROPDOWN("Menú desplegable", "Selecciona una opción"),
    MULTIPLE_CHOICE("Opción múltiple", "Selecciona una opción"),
    DATE("Fecha y hora", "Selecciona fecha y hora")
}

data class FieldOption(val id: String = java.util.UUID.randomUUID().toString(), val label: String)

val FormFieldType.hasOptions: Boolean
    get() = this in listOf(FormFieldType.MULTIPLE_CHOICE, FormFieldType.CHECKBOX_GROUP, FormFieldType.DROPDOWN)

data class FormFieldDraft(
    val id: String,
    val type: FormFieldType,
    val title: String,
    val required: Boolean = false,
    val options: List<FieldOption> = if (type.hasOptions) listOf(
        FieldOption(label = "Opción 1"), FieldOption(label = "Opción 2")
    ) else emptyList()
)

fun FormFieldDraft.hasValidOptions(): Boolean = !type.hasOptions ||
    (options.size >= 2 && options.all { it.label.isNotBlank() })
