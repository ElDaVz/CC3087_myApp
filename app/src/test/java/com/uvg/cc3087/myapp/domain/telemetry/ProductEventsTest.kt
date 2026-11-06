package com.uvg.cc3087.myapp.domain.telemetry

import com.uvg.cc3087.myapp.data.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ProductEventsTest {
    private val form = FormDraft("draft-00000000-0000-4000-8000-000000000001", "PRIVATE TITLE",
        listOf(FormFieldDraft("q", FormFieldType.TEXT, "PRIVATE QUESTION")), 1, 1,
        creationAttemptId = "00000000-0000-4000-8000-000000000002", creationStartedAt = 1000, templateType = "ORDER_FORM")
    @Test fun firstPublicationUsesConfirmationTimeAndNeverIncludesDomainText() = runTest {
        val events = mutableListOf<Pair<String,Map<String,Any>>>()
        val analytics = ProductAnalytics { name, _, parameters -> events += name to parameters }
        analytics.creationStarted(form.id, form.creationAttemptId!!)
        analytics.firstPublished(form, 12500, 1, "ORDER_FORM")
        assertEquals(listOf("form_creation_started", "form_published"), events.map { it.first })
        assertEquals(11.5, events.last().second["duration_seconds"])
        assertEquals(1L, events.last().second["field_count"])
        assertEquals(events.first().second["creation_attempt_id"], events.last().second["creation_attempt_id"])
        assertFalse(events.toString().contains("PRIVATE"))
    }
    @Test fun invalidDurationsAndUntrackedLegacyFormsDoNotEnterTheAverage() = runTest {
        var calls=0
        val analytics=ProductAnalytics { _,_,_->calls++ }
        analytics.firstPublished(form, 999, 1, "ORDER_FORM")
        analytics.firstPublished(form.copy(creationAttemptId=null), 10000, 1, "LEGACY")
        assertEquals(0,calls)
        assertNull(validDuration(1000,999));assertEquals(0.0,validDuration(1000,1000))
    }
    @Test fun telemetryFailureCannotUndoAConfirmedAction() = runTest {
        val analytics=ProductAnalytics { _,_,_->throw java.io.IOException("offline metrics") }
        analytics.firstPublished(form, 10000, 1, "ORDER_FORM")
    }
}
