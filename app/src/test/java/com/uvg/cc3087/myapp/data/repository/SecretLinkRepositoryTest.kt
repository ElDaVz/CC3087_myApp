package com.uvg.cc3087.myapp.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretLinkRepositoryTest {
    private class MemoryLinks : LinkStore {
        val saved = mutableMapOf<Pair<String, String>, FormLinks>()
        override suspend fun get(owner: String, formId: String) = saved[owner to formId]
        override suspend fun save(owner: String, formId: String, links: FormLinks) {
            saved[owner to formId] = links
        }
    }

    @Test fun secretsAreCreatedOnceAndScopedToOwner() = runTest {
        val store = MemoryLinks()
        val repository = SecretLinkRepository(store)
        val base = "https://forms.example"
        val first = repository.forForm("a", "f", "$base/f/f")
        val second = repository.forForm("a", "f", "$base/f/f")
        val other = repository.forForm("b", "f", "$base/f/f")

        assertEquals(first, second)
        assertNotEquals(first.singleUseUrl, other.singleUseUrl)
        assertNotEquals(first.trackingToken, other.trackingToken)
        assertTrue(first.trackingUrl!!.startsWith("$base/t/"))
        assertEquals(setOf("a" to "f", "b" to "f"), store.saved.keys)
    }
}
