package com.uvg.cc3087.myapp.domain.requests

import com.uvg.cc3087.myapp.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ResponseDashboardTest {
    private val zone = ZoneId.of("America/Guatemala")
    private val now = Instant.parse("2026-11-12T18:00:00Z")
    private fun row(id: String, date: String?, form: String = "f", status: RequestStatus? = RequestStatus.PENDING, read: Boolean = false) =
        RequestSummary(form, id, "Same title", date?.let(Instant::parse), requestStatus = status, isRead = read)

    @Test fun deduplicatesByBothIdsAndCountsEveryStatus() {
        val a = row("a", null)
        val result = responseDashboard(listOf(a, a, a.copy(formId = "other", requestStatus = RequestStatus.COMPLETED),
            a.copy(responseId = "b", requestStatus = RequestStatus.IN_PROGRESS)), ResponseFilter(period = ResponsePeriod.ALL), now, zone)
        assertEquals(3, result.requests.size)
        assertEquals(listOf(1, 1, 1), RequestStatus.entries.map { result.counts[it] })
        assertEquals(2, result.forms.size)
    }
    @Test fun weekIncludesSevenLocalDatesAndExcludesTomorrowAndMissingDates() {
        val rows = listOf(row("before", "2026-11-06T05:59:59Z"), row("start", "2026-11-06T06:00:00Z"),
            row("end", "2026-11-13T05:59:59Z"), row("tomorrow", "2026-11-13T06:00:00Z"), row("undated", null))
        val result = responseDashboard(rows, ResponseFilter(period = ResponsePeriod.WEEK), now, zone)
        assertEquals(setOf("start", "end"), result.requests.map { it.responseId }.toSet())
        assertEquals(7, result.buckets.size)
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 1), result.buckets.map { it.count })
    }
    @Test fun monthFiltersAndBadgeRemainIndependent() {
        val result = responseDashboard(listOf(row("a", "2026-11-12T18:00:00Z", read = true),
            row("old", "2025-01-01T18:00:00Z", "other")), ResponseFilter("f"), now, zone)
        assertEquals(listOf("a"), result.requests.map { it.responseId })
        assertTrue(result.hasUnread)
        assertEquals(30, result.buckets.size)
        assertEquals(1, result.buckets.sumOf { it.count })
    }
    @Test fun allGroupsMonthsAndPreservesUnknownStatusAndUndatedRows() {
        val result = responseDashboard(listOf(row("early", "2026-08-01T18:00:00Z"), row("late", "2026-10-31T18:00:00Z"),
            row("missing", null, status = null)), ResponseFilter(period = ResponsePeriod.ALL), now, zone)
        assertEquals(listOf(1, 0, 1, 0), result.buckets.map { it.count })
        assertEquals(3, result.requests.size)
        assertEquals(1, result.unknownStatusCount)
        assertEquals(1, result.undatedCount)
    }
    @Test fun daysUseCalendarBoundariesAcrossDaylightSaving() {
        val result = responseDashboard(listOf(row("inside", "2026-11-01T04:00:00Z"), row("outside", "2026-11-01T03:59:59Z")),
            ResponseFilter(period = ResponsePeriod.WEEK), Instant.parse("2026-11-07T18:00:00Z"), ZoneId.of("America/New_York"))
        assertEquals(listOf("inside"), result.requests.map { it.responseId })
    }
    @Test fun emptyPeriodDiffersFromEmptySourceAndHistoricalFormsRemainSelectable() {
        assertTrue(responseDashboard(emptyList(), ResponseFilter(), now, zone).sourceEmpty)
        val result = responseDashboard(listOf(row("old", "2024-01-01T18:00:00Z")), ResponseFilter(), now, zone)
        assertFalse(result.sourceEmpty)
        assertTrue(result.requests.isEmpty())
        assertEquals("f", result.forms.single().id)
    }
}
