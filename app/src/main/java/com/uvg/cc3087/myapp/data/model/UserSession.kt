package com.uvg.cc3087.myapp.data.model

data class UserSession(val userId: String, val email: String)
sealed interface AuthState {
    data object Initializing : AuthState
    data object Guest : AuthState
    data class SignedIn(val session: UserSession) : AuthState
}
val AuthState.userId: String? get() = (this as? AuthState.SignedIn)?.session?.userId
