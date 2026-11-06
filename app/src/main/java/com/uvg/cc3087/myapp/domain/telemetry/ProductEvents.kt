package com.uvg.cc3087.myapp.domain.telemetry

import com.uvg.cc3087.myapp.data.model.FormDraft
import kotlinx.coroutines.CancellationException

fun interface ProductAnalytics {
    suspend fun record(name: String, logicalId: String, parameters: Map<String, Any>)
}
object NoProductAnalytics : ProductAnalytics { override suspend fun record(name: String, logicalId: String, parameters: Map<String, Any>) {} }
fun validDuration(start: Long, end: Long): Double? = ((end.toDouble() - start.toDouble()) / 1000)
    .takeIf { it.isFinite() && it >= 0 }

suspend fun ProductAnalytics.creationStarted(formId: String, attempt: String) = safely {
    record("form_creation_started", "start:$attempt", mapOf("form_id" to formId,
        "creation_attempt_id" to attempt, "template_type" to "unselected"))
}
suspend fun ProductAnalytics.firstPublished(form: FormDraft, confirmedAt: Long, fieldCount: Int, template: String) = safely {
    val attempt = form.creationAttemptId ?: return@safely
    val start = form.creationStartedAt ?: return@safely
    val duration = validDuration(start, confirmedAt) ?: return@safely
    record("form_published", "publish:${form.id}", mapOf("form_id" to form.id, "creation_attempt_id" to attempt,
        "template_type" to template, "duration_seconds" to duration, "field_count" to fieldCount.toLong()))
}
private suspend fun safely(block: suspend () -> Unit) {
    try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { /* Metrics cannot undo a confirmed domain action. */ }
}
