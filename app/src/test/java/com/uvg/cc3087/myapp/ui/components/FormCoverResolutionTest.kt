package com.uvg.cc3087.myapp.ui.components

import org.junit.Assert.*
import org.junit.Test

class FormCoverResolutionTest {
    private val remote = "https://res.cloudinary.com/uxrkjzti/image/upload/v1/cover.jpg"
    @Test fun onlyKeepWithoutUsableLocalImageFallsBackToValidatedRemote() {
        assertEquals(remote, resolvePublishedCoverUrl("KEEP", false, remote))
        for (action in listOf("KEEP", "REMOVE", "REPLACE")) {
            assertNull(resolvePublishedCoverUrl(action, true, remote))
        }
        assertNull(resolvePublishedCoverUrl("REMOVE", false, remote))
        assertNull(resolvePublishedCoverUrl("REPLACE", false, remote))
        assertNull(resolvePublishedCoverUrl("KEEP", false, null))
        assertNull(resolvePublishedCoverUrl("KEEP", false, "https://example.com/image.jpg"))
    }
}
