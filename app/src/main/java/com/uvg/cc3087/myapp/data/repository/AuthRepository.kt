package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.AuthState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val configured: Boolean
    val state: StateFlow<AuthState>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String)
    suspend fun signOut()
}
