package com.uvg.cc3087.myapp.data.repository

/** UI-owned credential lookup. The repository only receives an already-verified ID token. */
interface GoogleCredentialProvider {
    val available: Boolean get() = true
    suspend fun idToken(): String?
}

object UnavailableGoogleCredentialProvider : GoogleCredentialProvider {
    override val available = false
    override suspend fun idToken(): String? = null
}
