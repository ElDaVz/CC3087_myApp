package com.uvg.cc3087.myapp.data.repository

import androidx.room.withTransaction
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.domain.validation.FormDraftValidator
import com.uvg.cc3087.myapp.domain.validation.FormValidationResult
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class RoomRequestRepository(private val database: FormLinkDatabase) : RequestRepository {
    private val dao get() = database.requests()
    override suspend fun getDetail(formId: String, responseId: String) = dao.getDetail(formId, responseId)?.toDetail()
    override suspend fun getRequests() = database.withTransaction {
        val read = database.requestReads().get("guest").map { it.formId to it.responseId }.toSet()
        dao.getRequests().map { it.toSummary().copy(isRead = (it.formId to it.responseId) in read) }
    }
    override fun observeRequests() = combine(dao.observeRequests(), database.requestReads().observe("guest")) { rows, reads ->
        val read = reads.map { it.formId to it.responseId }.toSet()
        rows.map { it.toSummary().copy(isRead = (it.formId to it.responseId) in read) }
    }
    override suspend fun markRead(formId: String, responseId: String) = database.withTransaction {
        checkNotNull(dao.getDetail(formId, responseId))
        database.requestReads().markRead(RequestReadEntity("guest", formId, responseId))
    }

    override suspend fun submit(form: FormDraft, responseId: String, answers: Map<String, FormAnswer>, locale: Locale): RequestDetail =
        database.withTransaction {
            require(responseId.isNotBlank())
            // Retrying a committed submission must not create a second response.
            dao.getDetail(form.id, responseId)?.let { return@withTransaction it.toDetail() }
            check(database.drafts().getDraft(form.id)?.toDraft() == form) { "The form changed; reopen it before submitting" }
            require(FormDraftValidator.validate(form.title, form.fields) == FormValidationResult.READY)
            require(answers.keys.all { id -> form.fields.any { it.id == id } })
            require(FormDraftValidator.validateAnswers(form.fields, answers, true, LocalDate.now(), locale).isEmpty())
            val snapshot = JSONArray()
            form.fields.forEach { field ->
                val answer = answers[field.id] ?: FormAnswer()
                val row = JSONObject().put("fieldId", field.id).put("label", field.title)
                when {
                    field.type.hasOptions -> row.put("kind", "choices").put("value",
                        JSONArray(field.options.filter { it.id in answer.selectedIds }.map { it.label }))
                    field.type == FormFieldType.CHECKBOX -> row.put("kind", "checked").put("value", answer.checked)
                    else -> row.put("kind", "text").put("value", when (field.type) {
                        FormFieldType.DATE -> listOf(answer.date, answer.time.takeIf { field.includeTime }.orEmpty())
                            .filter { it.isNotBlank() }.joinToString(" ")
                        else -> answer.text
                    })
                }
                snapshot.put(row)
            }
            val entity = RequestEntity(form.id, responseId, form.title, Instant.now().toEpochMilli(),
                RequestStatus.PENDING.name, snapshot.toString())
            dao.insert(entity)
            entity.toDetail()
        }

    override suspend fun updateStatus(formId: String, responseId: String, status: RequestStatus): RequestDetail =
        database.withTransaction {
            check(dao.updateStatus(formId, responseId, status.name) == 1) { "Request not found" }
            checkNotNull(dao.getDetail(formId, responseId)).toDetail()
        }
}

private fun RequestEntity.toSummary() = RequestSummary(formId, responseId, formTitle,
    Instant.ofEpochMilli(submittedAt), requestStatus = RequestStatus.valueOf(status))

private fun RequestEntity.toDetail(): RequestDetail {
    val rows = JSONArray(answersJson)
    val answers = (0 until rows.length()).map { index ->
        val row = rows.getJSONObject(index)
        val value = when (row.getString("kind")) {
            "checked" -> RequestAnswerValue.Checked(row.getBoolean("value"))
            "choices" -> row.getJSONArray("value").let { values ->
                RequestAnswerValue.Choices((0 until values.length()).map { values.getString(it) })
            }
            "text" -> RequestAnswerValue.Text(row.getString("value"))
            else -> error("Unknown stored answer type")
        }
        RequestDetailAnswer(row.getString("fieldId"), row.getString("label"), value)
    }
    return RequestDetail(formId, responseId, formTitle, Instant.ofEpochMilli(submittedAt), answers,
        requestStatus = RequestStatus.valueOf(status))
}
