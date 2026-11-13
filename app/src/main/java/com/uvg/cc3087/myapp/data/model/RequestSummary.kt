package com.uvg.cc3087.myapp.data.model

import java.time.Instant

data class RequestSummary(
    val formId: String,
    val responseId: String,
    val formTitle: String,
    val submittedAt: Instant?,
    val statusLabel: String? = null,
    val requestStatus: RequestStatus? = null,
    val isRead: Boolean = false
) {
    // A response ID is unique within its form, not across all forms.
    val key: String get() = "$formId/$responseId"
}

fun RequestDetail.toSummary() = RequestSummary(formId, responseId, formTitle, submittedAt, statusLabel, requestStatus)
