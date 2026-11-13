package com.uvg.cc3087.myapp.domain.requests

import com.uvg.cc3087.myapp.data.model.RequestStatus
import com.uvg.cc3087.myapp.data.model.RequestSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ResponsePeriod(val days: Long?) { WEEK(7), MONTH(30), ALL(null) }
data class ResponseFilter(val formId: String? = null, val period: ResponsePeriod = ResponsePeriod.MONTH)
data class ResponseBucket(val date: LocalDate, val count: Int)
data class ResponseForm(val id: String, val title: String)
data class ResponseDashboard(
    val requests: List<RequestSummary> = emptyList(),
    val counts: Map<RequestStatus, Int> = emptyMap(),
    val unknownStatusCount: Int = 0,
    val buckets: List<ResponseBucket> = emptyList(),
    val forms: List<ResponseForm> = emptyList(),
    val hasUnread: Boolean = false,
    val sourceEmpty: Boolean = true,
    val undatedCount: Int = 0
)

/** One projection for dashboard and inbox; no customer answers or Analytics are read. */
fun responseDashboard(source: List<RequestSummary>, filter: ResponseFilter, now: Instant, zone: ZoneId): ResponseDashboard {
    val requests = source.distinctBy { it.key }.sortedWith(
        compareByDescending<RequestSummary> { it.submittedAt }.thenBy { it.formId }.thenBy { it.responseId })
    val today = now.atZone(zone).toLocalDate()
    val start = filter.period.days?.let { today.minusDays(it - 1).atStartOfDay(zone).toInstant() }
    val end = today.plusDays(1).atStartOfDay(zone).toInstant()
    val selected = requests.filter { request ->
        (filter.formId == null || filter.formId == request.formId) &&
            (start == null || request.submittedAt?.let { it >= start && it < end } == true)
    }
    val dates = selected.mapNotNull { it.submittedAt?.atZone(zone)?.toLocalDate() }
    val buckets = if (filter.period == ResponsePeriod.ALL) {
        val counts = dates.groupingBy { it.withDayOfMonth(1) }.eachCount()
        val first = counts.keys.minOrNull()
        val last = maxOf(today.withDayOfMonth(1), counts.keys.maxOrNull() ?: today.withDayOfMonth(1))
        if (first == null) emptyList() else generateSequence(first) { it.plusMonths(1) }.takeWhile { it <= last }
            .map { ResponseBucket(it, counts[it] ?: 0) }.toList()
    } else {
        val counts = dates.groupingBy { it }.eachCount()
        (0 until filter.period.days!!).map { offset ->
            val date = today.minusDays(filter.period.days - 1 - offset)
            ResponseBucket(date, counts[date] ?: 0)
        }
    }
    return ResponseDashboard(selected, RequestStatus.entries.associateWith { status -> selected.count { it.requestStatus == status } },
        selected.count { it.requestStatus == null }, buckets,
        requests.distinctBy { it.formId }.map { ResponseForm(it.formId, it.formTitle) },
        requests.any { !it.isRead }, requests.isEmpty(), selected.count { it.submittedAt == null })
}
