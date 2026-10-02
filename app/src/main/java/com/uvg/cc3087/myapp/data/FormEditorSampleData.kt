package com.uvg.cc3087.myapp.data

import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType

// estos campos dejan visible el flujo completo mientras llega el backend :D
object FormEditorSampleData {
    val templateFields = listOf(
        FormFieldDraft(
            id = "field-1",
            type = FormFieldType.TEXT,
            title = "Nombre completo",
            required = true
        ),
        FormFieldDraft(
            id = "field-2",
            type = FormFieldType.MULTIPLE_CHOICE,
            title = "Tipo de solicitud",
            required = true
        ),
        FormFieldDraft(
            id = "field-3",
            type = FormFieldType.DATE,
            title = "Fecha preferida"
        )
    )
}
