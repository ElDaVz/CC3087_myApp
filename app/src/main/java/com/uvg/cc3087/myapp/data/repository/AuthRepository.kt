package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.AuthState
import com.uvg.cc3087.myapp.data.model.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    suspend fun signIn(email: String, password: String): Result<UserSession>
    suspend fun signUp(name: String, email: String, password: String): Result<UserSession>
    suspend fun continueAsGuest(): UserSession
    suspend fun signOut()
}
