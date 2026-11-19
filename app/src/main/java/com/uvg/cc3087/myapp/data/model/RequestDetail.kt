package com.uvg.cc3087.myapp.data.model

import java.time.Instant

/** Read-only projection of a submitted response, separate from editable form drafts. */
data class RequestDetail(
    val formId: String,
    val responseId: String,
    val formTitle: String,
    val submittedAt: Instant?,
    val answers: List<RequestDetailAnswer>,
    val statusLabel: String? = null,
    val requestStatus: RequestStatus? = null
)

data class RequestDetailAnswer(
    val fieldId: String,
    val label: String,
    val value: RequestAnswerValue,
    val field: RequestFieldSnapshot? = null,
    val numericValue: java.math.BigDecimal? = null
)

// estos datos pertenecen a la pregunta recibida, no al borrador que se edita después
data class RequestFieldSnapshot(
    val type: FormFieldType,
    val optionLabels: List<String> = emptyList(),
    val unit: String = "",
    val includeTime: Boolean = false,
    val multipleAnswers: Boolean = type == FormFieldType.CHECKBOX_GROUP
)

sealed interface RequestAnswerValue {
    /** Includes text, numbers and dates formatted by the backend adapter. */
    data class Text(val value: String) : RequestAnswerValue
    /** Resolved option labels, never option IDs. */
    data class Choices(val labels: List<String>) : RequestAnswerValue
    data class Checked(val value: Boolean) : RequestAnswerValue
    data class Attachments(val files: List<RequestAttachment>) : RequestAnswerValue
}

data class RequestAttachment(val name: String, val storageReference: String)
