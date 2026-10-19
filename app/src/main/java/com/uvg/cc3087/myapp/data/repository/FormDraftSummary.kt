package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.model.FormStatus
import com.uvg.cc3087.myapp.data.model.FormSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun FormDraft.toSummary() = FormSummary(
    id = id,
    title = title,
    responseCount = 0,
    updatedDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(updatedAt)),
    status = FormStatus.DRAFT,
    imageUrl = ""
)
