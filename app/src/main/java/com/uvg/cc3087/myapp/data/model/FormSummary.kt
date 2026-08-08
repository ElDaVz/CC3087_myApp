package com.uvg.cc3087.myapp.data.model

enum class FormStatus {
    ACTIVE,
    DRAFT
}

data class FormSummary(
    val id: String,
    val title: String,
    val responseCount: Int,
    val updatedDate: String,
    val status: FormStatus,
    val imageUrl: String
)
