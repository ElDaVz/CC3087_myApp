package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleFormRepositoryTest {
    @Test
    fun repeatedSubscriptionsKeepSampleIdsAndOrder() = runTest {
        val repository = SampleFormRepository()

        val firstList = repository.observeForms().first()
        val secondList = repository.observeForms().first()

        assertEquals(10, firstList.size)
        assertEquals("bakery-custom-order", firstList.first().id)
        assertEquals(firstList.map { it.id }, secondList.map { it.id })
        assertEquals(firstList.size, firstList.map { it.id }.distinct().size)
    }
}
