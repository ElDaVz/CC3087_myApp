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
    val value: RequestAnswerValue
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
