package com.uvg.cc3087.myapp.data.model

data class UserSession(
    val userId: String,
    val email: String,
    val displayName: String,
    val isGuest: Boolean = false
)

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val session: UserSession) : AuthState
}
