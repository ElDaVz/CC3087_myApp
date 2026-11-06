package com.uvg.cc3087.myapp.data.model

// un borrador puede estar incompleto, por eso no validamos sus preguntas al guardarlo
data class FormDraft(
    val id: String,
    val title: String,
    val fields: List<FormFieldDraft>,
    val createdAt: Long,
    val updatedAt: Long,
    val coverPath: String? = null,
    val ownerId: String? = null,
    val creationAttemptId: String? = null,
    val creationStartedAt: Long? = null,
    val templateType: String? = null
)
