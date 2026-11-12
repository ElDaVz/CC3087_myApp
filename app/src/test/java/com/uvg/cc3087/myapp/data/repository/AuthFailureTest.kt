package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.remote.http.BackendHttpException
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthFailureTest {
    @Test fun mapsServerCodesToSafeCategories() {
        val codes = mapOf(
            "INVALID_LOGIN_CREDENTIALS" to AuthFailure.INVALID_CREDENTIALS,
            "INVALID_PASSWORD" to AuthFailure.INVALID_CREDENTIALS,
            "EMAIL_NOT_FOUND" to AuthFailure.INVALID_CREDENTIALS,
            "EMAIL_EXISTS" to AuthFailure.EMAIL_EXISTS,
            "INVALID_EMAIL" to AuthFailure.INVALID_EMAIL,
            "WEAK_PASSWORD" to AuthFailure.WEAK_PASSWORD,
            "USER_DISABLED" to AuthFailure.DISABLED,
            "TOO_MANY_ATTEMPTS_TRY_LATER" to AuthFailure.TOO_MANY_ATTEMPTS,
            "RESET_PASSWORD_EXCEED_LIMIT" to AuthFailure.TOO_MANY_ATTEMPTS,
            "UNKNOWN_CODE" to AuthFailure.UNEXPECTED)
        codes.forEach { (code, expected) -> assertEquals(expected, authFailure(BackendHttpException(400, code))) }
        assertEquals(AuthFailure.NETWORK, authFailure(java.net.SocketTimeoutException()))
        assertEquals(AuthFailure.NETWORK, authFailure(java.net.UnknownHostException()))
        assertEquals(AuthFailure.UNEXPECTED, authFailure(IllegalStateException("private server detail")))
    }
}
