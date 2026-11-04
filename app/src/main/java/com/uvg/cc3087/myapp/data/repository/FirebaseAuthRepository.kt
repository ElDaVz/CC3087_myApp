package com.uvg.cc3087.myapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.uvg.cc3087.myapp.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** Firebase owns the session and credentials; no session token or password is stored in Room. */
class FirebaseAuthRepository(private val auth: FirebaseAuth?) : AuthRepository {
    override val configured get() = auth != null
    private val mutableState = MutableStateFlow<AuthState>(if (auth == null) AuthState.Guest else AuthState.Initializing)
    override val state = mutableState.asStateFlow()
    private val listener = FirebaseAuth.AuthStateListener { firebase ->
        mutableState.value = firebase.currentUser?.let {
            AuthState.SignedIn(UserSession(it.uid, it.email.orEmpty()))
        } ?: AuthState.Guest
    }
    init { auth?.addAuthStateListener(listener) }
    override suspend fun signIn(email: String, password: String) {
        checkNotNull(auth) { "Firebase is not configured" }.signInWithEmailAndPassword(email.trim(), password).await()
    }
    override suspend fun signUp(email: String, password: String) {
        checkNotNull(auth) { "Firebase is not configured" }.createUserWithEmailAndPassword(email.trim(), password).await()
    }
    override suspend fun signOut() { auth?.signOut() }
}
