package com.uvg.cc3087.myapp.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.uvg.cc3087.myapp.data.repository.GoogleCredentialProvider

/** Uses the system credential selector; no Firebase SDK session is created here. */
class CredentialManagerGoogleProvider(
    private val activityContext: Context,
    private val serverClientId: String?
) : GoogleCredentialProvider {
    private val manager = CredentialManager.create(activityContext)
    override val available: Boolean get() = !serverClientId.isNullOrBlank()

    override suspend fun idToken(): String? {
        val client = serverClientId?.takeIf { it.isNotBlank() } ?: return null
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(client)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        return try {
            val response = manager.getCredential(activityContext, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val custom = response.credential as? CustomCredential
            if (custom?.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) null
            else GoogleIdTokenCredential.createFrom(custom.data).idToken.takeIf { it.isNotBlank() }
        } catch (_: GetCredentialCancellationException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
