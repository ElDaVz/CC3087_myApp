package com.uvg.cc3087.myapp.data.repository

import java.security.SecureRandom

/** Invitation visibility. Tracking links are always secret and owner-only. */
enum class InvitationMode { OPEN, SINGLE_USE }

data class FormLinks(
    val openUrl: String,
    val singleUseUrl: String? = null,
    val trackingToken: String? = null
) {
    val trackingUrl: String? get() = tokenUrl(base = openUrl.substringBeforeLast("/f/"), token = trackingToken, kind = "t")
    val shareUrl: String get() = singleUseUrl ?: openUrl

    fun withBase(base: String): FormLinks {
        val formId = openUrl.substringAfterLast('/')
        val root = base.trimEnd('/')
        return copy(
            openUrl = "$root/f/$formId",
            singleUseUrl = singleUseUrl?.substringAfterLast('/')?.let { "$root/i/$it" }
        )
    }
}

/** Opaque, high-entropy URL tokens. Never logged or stored in public form fields. */
internal object LinkTokens {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private val random = SecureRandom()

    fun newToken(bytes: Int = 24): String {
        val source = ByteArray(bytes)
        random.nextBytes(source)
        val result = StringBuilder(ceilDiv(bytes * 8, 6))
        var buffer = 0
        var bits = 0
        for (value in source) {
            buffer = (buffer shl 8) or (value.toInt() and 0xff)
            bits += 8
            while (bits >= 6) {
                bits -= 6
                result.append(ALPHABET[(buffer shr bits) and 0x3f])
            }
        }
        if (bits > 0) result.append(ALPHABET[(buffer shl (6 - bits)) and 0x3f])
        return result.toString()
    }

    fun isValid(token: String): Boolean =
        token.length in 20..64 && token.all { it in ALPHABET }
}

internal fun tokenUrl(base: String, token: String?, kind: String): String? {
    val value = token?.takeIf(LinkTokens::isValid) ?: return null
    return "${base.trimEnd('/')}/$kind/$value"
}

internal fun ceilDiv(value: Int, divisor: Int) = (value + divisor - 1) / divisor
