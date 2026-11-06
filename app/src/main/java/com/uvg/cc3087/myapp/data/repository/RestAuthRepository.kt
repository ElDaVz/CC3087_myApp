package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/** Firebase Authentication via HTTPS; no SDK plaintext session store is created. */
class RestAuthRepository(private val apiKey: String?, private val cipher: DeviceCipher,
    private val http: FirebaseHttp, private val emulator: Boolean = false,
    private val now: () -> Long = System::currentTimeMillis) : AuthRepository {
    override val configured get() = apiKey != null
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Initializing)
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var session: JSONObject? = null
    private var generation = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val base get() = if (emulator) "http://10.0.2.2:9099/identitytoolkit.googleapis.com" else "https://identitytoolkit.googleapis.com"
    init { scope.launch {
        mutex.withLock {
            session = try { cipher.read("auth-session")?.let { JSONObject(String(it)) } } catch (_: Exception) { null }
            mutableState.value = session?.let { AuthState.SignedIn(UserSession(it.getString("uid"), it.getString("email"))) } ?: AuthState.Guest
        }
    } }
    override suspend fun signIn(email: String, password: String) = authenticate("signInWithPassword", email, password)
    override suspend fun signUp(email: String, password: String) = authenticate("signUp", email, password)
    private suspend fun authenticate(action: String, email: String, password: String) {
        val key = checkNotNull(apiKey) { "Firebase is not configured" }
        val stamp = generation
        val result = http.request("$base/v1/accounts:$action?key=$key", JSONObject()
            .put("email", email.trim()).put("password", password).put("returnSecureToken", true))
        mutex.withLock {
            check(stamp == generation) { "Session changed" }
            persist(JSONObject().put("uid", result.getString("localId")).put("email", result.getString("email"))
                .put("idToken", result.getString("idToken")).put("refreshToken", result.getString("refreshToken"))
                .put("expiresAt", now() + result.getString("expiresIn").toLong() * 1000))
        }
    }
    private fun persist(value: JSONObject) {
        cipher.write("auth-session", value.toString().toByteArray())
        session = value
        mutableState.value = AuthState.SignedIn(UserSession(value.getString("uid"), value.getString("email")))
    }
    suspend fun token(owner: String): String = mutex.withLock {
        val value = checkNotNull(session) { "Not authenticated" }
        check(value.getString("uid") == owner) { "Session changed" }
        if (value.getLong("expiresAt") < now() + 60_000) {
            val host = if (emulator) "http://10.0.2.2:9099/securetoken.googleapis.com" else "https://securetoken.googleapis.com"
            try {
                val result = http.request("$host/v1/token?key=$apiKey", JSONObject().put("grant_type", "refresh_token").put("refresh_token", value.getString("refreshToken")))
                persist(JSONObject(value.toString()).put("idToken", result.getString("id_token"))
                    .put("refreshToken", result.getString("refresh_token"))
                    .put("expiresAt", now() + result.getString("expires_in").toLong() * 1000))
            } catch (error: BackendHttpException) {
                if (error.status == 400 || error.status == 401) { session = null; cipher.remove("auth-session"); mutableState.value = AuthState.Guest; generation++ }
                throw error
            }
        }
        checkNotNull(session).getString("idToken")
    }
    override suspend fun signOut() { mutex.withLock {
        generation++; session = null; cipher.remove("auth-session"); mutableState.value = AuthState.Guest
    } }
}
