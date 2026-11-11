package com.uvg.cc3087.myapp.domain.validation

import com.uvg.cc3087.myapp.data.model.CoverAsset
import org.junit.Assert.*
import org.junit.Test

class CoverAssetTest {
    private val url = "https://res.cloudinary.com/uxrkjzti/image/upload/v123/formlink/covers/random.jpg"
    @Test fun acceptsOnlyTheConfiguredHttpsImageEnvironment() {
        assertTrue(CoverAsset.allowedUrl(url))
        for (bad in listOf(url.replace("https:", "http:"), url.replace("uxrkjzti", "another"),
            url.replace("res.cloudinary.com", "res.cloudinary.com.evil.example"), "$url?token=secret",
            url.replace("/image/", "/video/"), url.replace("random.jpg", "../random.jpg"),
            url.replace("random.jpg", "random.svg"), url.replace("res.cloudinary.com", "user@res.cloudinary.com")))
            assertFalse(bad, CoverAsset.allowedUrl(bad))
    }
    @Test fun metadataRequiresValidHashAndPublicId() {
        assertNull(CoverAsset.fromWire(null))
        val good = mapOf("url" to url, "publicId" to "formlink/covers/random", "hash" to "a".repeat(64))
        assertEquals(url, CoverAsset.fromWire(good)!!.url)
        assertTrue(runCatching { CoverAsset.fromWire(good + ("hash" to "private free text")) }.isFailure)
        assertTrue(runCatching { CoverAsset.fromWire(good + ("publicId" to "invalid?")) }.isFailure)
    }
}
