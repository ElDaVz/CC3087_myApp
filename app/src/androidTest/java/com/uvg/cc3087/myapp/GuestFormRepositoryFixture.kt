package com.uvg.cc3087.myapp

import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.local.RoomFormStore
import com.uvg.cc3087.myapp.data.model.AuthState
import com.uvg.cc3087.myapp.data.repository.AuthRepository
import com.uvg.cc3087.myapp.data.repository.CoverStorage
import com.uvg.cc3087.myapp.data.repository.OfflineFirstFormRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Production persistence and session boundary, with no access to the installed app's session. */
fun FormLinkDatabase.guestFormRepository(covers: CoverStorage? = null): OfflineFirstFormRepository {
    val guest = object : AuthRepository {
        override val configured = false
        override val state = MutableStateFlow<AuthState>(AuthState.Guest)
        override suspend fun signIn(email: String, password: String) = error("Guest fixture")
        override suspend fun signUp(email: String, password: String) = error("Guest fixture")
        override suspend fun sendPasswordReset(email: String) = error("Guest fixture")
        override suspend fun signOut() = Unit
    }
    return OfflineFirstFormRepository(RoomFormStore(this, covers), guest, null) {
        error("A guest must never schedule account synchronization")
    }
}
