package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.AuthState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val configured: Boolean
    val state: StateFlow<AuthState>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun sendPasswordReset(email: String)
    suspend fun signOut()
}

/** Safe categories shared with the UI; server payloads never cross this boundary. */
enum class AuthFailure { INVALID_CREDENTIALS, EMAIL_EXISTS, INVALID_EMAIL, WEAK_PASSWORD, DISABLED, TOO_MANY_ATTEMPTS, NETWORK, UNEXPECTED }
class AuthException(val reason: AuthFailure) : Exception("Authentication failed")

internal fun authFailure(error: Exception): AuthFailure = when (error) {
    is com.uvg.cc3087.myapp.data.remote.http.BackendHttpException -> when (error.code) {
        "INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD", "EMAIL_NOT_FOUND", "INVALID_CREDENTIAL" -> AuthFailure.INVALID_CREDENTIALS
        "EMAIL_EXISTS" -> AuthFailure.EMAIL_EXISTS
        "INVALID_EMAIL" -> AuthFailure.INVALID_EMAIL
        "WEAK_PASSWORD" -> AuthFailure.WEAK_PASSWORD
        "USER_DISABLED" -> AuthFailure.DISABLED
        "TOO_MANY_ATTEMPTS_TRY_LATER", "TOO_MANY_ATTEMPTS", "RESET_PASSWORD_EXCEED_LIMIT" -> AuthFailure.TOO_MANY_ATTEMPTS
        else -> AuthFailure.UNEXPECTED
    }
    is java.io.IOException -> AuthFailure.NETWORK
    else -> AuthFailure.UNEXPECTED
}
