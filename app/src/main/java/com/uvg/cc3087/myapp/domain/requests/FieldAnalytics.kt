package com.uvg.cc3087.myapp.domain.requests

import com.uvg.cc3087.myapp.data.model.*
import java.math.BigDecimal
import java.math.MathContext
import java.time.*

data class AnswerCategory(val label: String, val count: Int)

sealed interface AnswerStatistics {
    data class Text(val averageLength: Double?) : AnswerStatistics
    data class Numeric(val minimum: BigDecimal?, val maximum: BigDecimal?, val average: BigDecimal?) : AnswerStatistics
    data class Email(val valid: Int) : AnswerStatistics
    data class Selection(val categories: List<AnswerCategory>, val multiple: Boolean, val valid: Int) : AnswerStatistics
    data class Checkbox(val yes: Int, val no: Int) : AnswerStatistics
    data class Dates(val first: LocalDateTime?, val last: LocalDateTime?, val days: List<AnswerCategory>,
        val hours: List<AnswerCategory>, val valid: Int) : AnswerStatistics
    data object Legacy : AnswerStatistics
}

data class FieldAnalytics(
    val fieldId: String,
    val label: String,
    val snapshot: RequestFieldSnapshot?,
    val version: Int,
    val hasVersions: Boolean,
    val total: Int,
    val answered: Int,
    val invalid: Int,
    val statistics: AnswerStatistics
) { val empty: Int get() = total - answered }

data class FormAnalytics(
    val responseCount: Int = 0,
    val excludedResponses: Int = 0,
    val fields: List<FieldAnalytics> = emptyList()
)

private data class FieldKey(val id: String, val label: String, val snapshot: RequestFieldSnapshot?)

// nunca consultamos el borrador actual: cada versión se calcula desde su snapshot histórico
fun formAnalytics(formId: String, source: List<RequestDetail>, period: ResponsePeriod, now: Instant, zone: ZoneId): FormAnalytics {
    require(source.all { it.formId == formId })
    val today = now.atZone(zone).toLocalDate()
    val start = period.days?.let { today.minusDays(it - 1).atStartOfDay(zone).toInstant() }
    val end = today.plusDays(1).atStartOfDay(zone).toInstant()
    val selected = source.distinctBy { it.formId to it.responseId }.filter {
        start == null || it.submittedAt?.let { time -> time >= start && time < end } == true
    }.sortedWith(compareByDescending<RequestDetail> { it.submittedAt }.thenBy { it.responseId })
    val usable = selected.filter { row -> row.answers.map { it.fieldId }.distinct().size == row.answers.size }
    val groups = usable.flatMap { it.answers }.groupBy { FieldKey(it.fieldId, it.label, it.field) }
    val versions = groups.keys.groupBy { it.id }
    val fields = groups.map { (key, answers) ->
        val nonEmpty = answers.filterNot { it.value.isEmpty() }
        var invalid = 0
        val stats = when (key.snapshot?.type) {
            FormFieldType.TEXT, FormFieldType.PARAGRAPH -> {
                val values = nonEmpty.mapNotNull { (it.value as? RequestAnswerValue.Text)?.value }
                invalid = nonEmpty.size - values.size
                AnswerStatistics.Text(values.takeIf { it.isNotEmpty() }?.map { it.codePointCount(0, it.length) }?.average())
            }
            FormFieldType.NUMBER -> {
                val values = nonEmpty.mapNotNull { answer ->
                    // el valor canónico evita interpretar 1,5 de otra región como 15
                    if (answer.value is RequestAnswerValue.Text) answer.numericValue else null
                }
                invalid = nonEmpty.size - values.size
                AnswerStatistics.Numeric(values.minOrNull(), values.maxOrNull(),
                    if (values.isEmpty()) null else values.fold(BigDecimal.ZERO, BigDecimal::add)
                        .divide(values.size.toBigDecimal(), MathContext.DECIMAL128))
            }
            FormFieldType.EMAIL -> {
                val valid = nonEmpty.count { answer ->
                    (answer.value as? RequestAnswerValue.Text)?.value?.trim()?.let {
                        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(it)
                    } == true
                }
                invalid = nonEmpty.size - valid
                AnswerStatistics.Email(valid)
            }
            FormFieldType.CHECKBOX -> {
                val values = nonEmpty.mapNotNull { (it.value as? RequestAnswerValue.Checked)?.value }
                invalid = nonEmpty.size - values.size
                AnswerStatistics.Checkbox(values.count { it }, values.count { !it })
            }
            FormFieldType.MULTIPLE_CHOICE, FormFieldType.CHECKBOX_GROUP, FormFieldType.DROPDOWN -> {
                val config = checkNotNull(key.snapshot)
                val values = nonEmpty.mapNotNull { answer ->
                    (answer.value as? RequestAnswerValue.Choices)?.labels?.takeIf { labels ->
                        labels.all { it in config.optionLabels } && (config.multipleAnswers || labels.distinct().size <= 1)
                    }?.distinct()
                }
                invalid = nonEmpty.size - values.size
                val counts = values.flatten().groupingBy { it }.eachCount()
                AnswerStatistics.Selection(config.optionLabels.distinct().map { AnswerCategory(it, counts[it] ?: 0) }
                    .sortedWith(compareByDescending<AnswerCategory> { it.count }.thenBy { it.label }), config.multipleAnswers, values.size)
            }
            FormFieldType.DATE -> {
                val config = checkNotNull(key.snapshot)
                val values = nonEmpty.mapNotNull { answer ->
                    (answer.value as? RequestAnswerValue.Text)?.value?.let { raw -> runCatching {
                        if (config.includeTime) LocalDateTime.parse(raw.replace(' ', 'T'))
                        else LocalDate.parse(raw).atStartOfDay()
                    }.getOrNull() }
                }
                invalid = nonEmpty.size - values.size
                val days = values.groupingBy { it.toLocalDate().toString() }.eachCount()
                    .map { AnswerCategory(it.key, it.value) }.sortedBy { it.label }
                val hours = if (!config.includeTime) emptyList() else values.groupingBy { it.hour }.eachCount()
                    .toSortedMap().map { AnswerCategory("%02d:00".format(java.util.Locale.ROOT, it.key), it.value) }
                AnswerStatistics.Dates(values.minOrNull(), values.maxOrNull(), days, hours, values.size)
            }
            null -> AnswerStatistics.Legacy
        }
        val siblings = checkNotNull(versions[key.id])
        FieldAnalytics(key.id, key.label, key.snapshot, siblings.indexOf(key) + 1, siblings.size > 1,
            answers.size, nonEmpty.size, invalid, stats)
    }
    return FormAnalytics(selected.size, selected.size - usable.size, fields)
}

private fun RequestAnswerValue.isEmpty(): Boolean = when (this) {
    is RequestAnswerValue.Text -> value.isBlank()
    is RequestAnswerValue.Choices -> labels.isEmpty() || labels.all { it.isBlank() }
    is RequestAnswerValue.Checked -> false
    is RequestAnswerValue.Attachments -> files.isEmpty()
}
