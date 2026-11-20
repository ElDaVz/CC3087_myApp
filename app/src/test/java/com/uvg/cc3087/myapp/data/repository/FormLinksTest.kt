package com.uvg.cc3087.myapp.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormLinksTest {
    @Test fun generatedTokensAreOpaqueUniqueAndUrlSafe() {
        val first = LinkTokens.newToken()
        val second = LinkTokens.newToken()
        assertNotEquals(first, second)
        assertTrue(first.length >= 32)
        assertTrue(LinkTokens.isValid(first))
        assertTrue(LinkTokens.isValid(second))
    }

    @Test fun trackingUrlKeepsTheSecretOutOfThePublicFormPath() {
        val token = LinkTokens.newToken()
        val links = FormLinks("https://forms.example/f/f1", trackingToken = token)
        assertTrue(links.trackingUrl!!.startsWith("https://forms.example/t/"))
        assertTrue(!links.trackingUrl!!.contains("/f/"))
        assertNull(FormLinks("https://forms.example/f/f1", trackingToken = "short").trackingUrl)
    }

    @Test fun singleUseInvitationIsTheDefaultSharedLink() {
        val links = FormLinks(
            "https://forms.example/f/f1",
            "https://forms.example/i/abc",
            LinkTokens.newToken()
        )
        assertEquals("https://forms.example/i/abc", links.shareUrl)
        assertEquals("https://other.example/f/f1", links.withBase("https://other.example").openUrl)
    }
}
