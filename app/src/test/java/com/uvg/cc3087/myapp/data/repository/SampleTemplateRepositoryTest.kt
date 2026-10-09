package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.TemplateType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleTemplateRepositoryTest {
    @Test
    fun repeatedSubscriptionsKeepTheFourTemplatesAndTheirIds() = runTest {
        val repository = SampleTemplateRepository()
        val firstRead = repository.observeTemplates().first()
        val secondRead = repository.observeTemplates().first()

        assertEquals(
            listOf("job-application", "order-form", "event-rsvp", "feedback"),
            firstRead.map { it.id }
        )
        assertEquals(
            listOf(
                TemplateType.JOB_APPLICATION,
                TemplateType.ORDER_FORM,
                TemplateType.EVENT_RSVP,
                TemplateType.FEEDBACK
            ),
            firstRead.map { it.type }
        )
        assertEquals(4, firstRead.map { it.id }.distinct().size)
        assertEquals(firstRead, secondRead)
    }
}
