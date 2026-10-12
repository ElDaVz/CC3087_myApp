package com.uvg.cc3087.myapp.data

import android.content.res.Resources
import com.uvg.cc3087.myapp.R
import com.uvg.cc3087.myapp.data.model.FieldOption
import com.uvg.cc3087.myapp.data.model.FormFieldDraft
import com.uvg.cc3087.myapp.data.model.FormFieldType

// estos campos dejan visible el flujo completo mientras llega el backend :D
object FormEditorSampleData {
    fun templateFields(resources: Resources) = listOf(
        FormFieldDraft(
            id = "field-1",
            type = FormFieldType.TEXT,
            title = resources.getString(R.string.sample_name),
            required = true
        ),
        FormFieldDraft(
            id = "field-2",
            type = FormFieldType.MULTIPLE_CHOICE,
            title = resources.getString(R.string.sample_request_type),
            required = true,
            options = listOf(
                FieldOption("request-order", resources.getString(R.string.sample_request_order)),
                FieldOption("request-booking", resources.getString(R.string.sample_request_booking))
            )
        ),
        FormFieldDraft(
            id = "field-3",
            type = FormFieldType.DATE,
            title = resources.getString(R.string.sample_preferred_date),
            includeTime = false
        )
    )
}
