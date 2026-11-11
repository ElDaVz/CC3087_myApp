package com.uvg.cc3087.myapp.data.model

import java.net.URI

/** Only public image metadata; credentials and local paths never leave the device. */
data class CoverAsset(val url: String, val publicId: String, val hash: String) {
    fun toWire(): Map<String, Any> = mapOf("url" to url, "publicId" to publicId, "hash" to hash)
    companion object {
        const val CLOUD = "uxrkjzti"
        fun allowedUrl(value: String): Boolean = runCatching {
            val uri = URI(value)
            uri.scheme == "https" && uri.host == "res.cloudinary.com" && uri.port == -1 &&
                uri.userInfo == null && uri.query == null && uri.fragment == null &&
                uri.rawPath.matches(Regex("/$CLOUD/image/upload/[A-Za-z0-9_/-]+[.](jpg|jpeg|png|webp)")) &&
                !uri.rawPath.contains("..")
        }.getOrDefault(false)
        fun fromWire(value: Any?): CoverAsset? {
            if (value == null) return null
            val map = value as? Map<*, *> ?: error("Invalid cover metadata")
            val asset = CoverAsset(map["url"] as String, map["publicId"] as String, map["hash"] as String)
            require(allowedUrl(asset.url) && asset.publicId.matches(Regex("[A-Za-z0-9_/-]{1,240}")) &&
                asset.hash.matches(Regex("[a-f0-9]{64}")))
            return asset
        }
    }
}
