package com.uvg.cc3087.myapp.data.repository

import android.content.Context
import com.uvg.cc3087.myapp.data.local.AppPreferencesManager
import com.uvg.cc3087.myapp.data.model.AuthState
import com.uvg.cc3087.myapp.data.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class LocalAuthRepository(context: Context) : AuthRepository {

    private val prefsManager = AppPreferencesManager(context.applicationContext)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val userId = prefsManager.activeUserId.firstOrNull()
            val email = prefsManager.activeUserEmail.firstOrNull()
            val name = prefsManager.activeUserName.firstOrNull()

            if (!userId.isNullOrBlank()) {
                val isGuest = userId.startsWith("guest_")
                _authState.value = AuthState.Authenticated(
                    UserSession(
                        userId = userId,
                        email = email ?: if (isGuest) "invitado@formlink.app" else "usuario@formlink.app",
                        displayName = name ?: if (isGuest) "Invitado" else "Usuario",
                        isGuest = isGuest
                    )
                )
            }
        }
    }

    override fun observeAuthState(): Flow<AuthState> = _authState.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres"))
        }

        val name = trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
        val userId = "usr_" + UUID.nameUUIDFromBytes(trimmedEmail.toByteArray()).toString().take(8)
        val session = UserSession(
            userId = userId,
            email = trimmedEmail,
            displayName = name,
            isGuest = false
        )

        prefsManager.saveUserSession(userId = userId, email = trimmedEmail, name = name)
        _authState.value = AuthState.Authenticated(session)
        Result.success(session)
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Ingresa tu nombre completo"))
        }
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres"))
        }

        val userId = "usr_" + UUID.randomUUID().toString().take(8)
        val session = UserSession(
            userId = userId,
            email = trimmedEmail,
            displayName = trimmedName,
            isGuest = false
        )

        prefsManager.saveUserSession(userId = userId, email = trimmedEmail, name = trimmedName)
        _authState.value = AuthState.Authenticated(session)
        Result.success(session)
    }

    override suspend fun continueAsGuest(): UserSession = withContext(Dispatchers.IO) {
        val guestId = "guest_" + UUID.randomUUID().toString().take(6)
        val session = UserSession(
            userId = guestId,
            email = "invitado@formlink.app",
            displayName = "Invitado",
            isGuest = true
        )

        prefsManager.saveUserSession(userId = guestId, email = session.email, name = session.displayName)
        _authState.value = AuthState.Authenticated(session)
        session
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        prefsManager.saveUserSession(userId = null, email = null, name = null)
        _authState.value = AuthState.Unauthenticated
    }
}
