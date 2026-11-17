package com.uvg.cc3087.myapp.data.local

import androidx.room.withTransaction
import com.uvg.cc3087.myapp.data.repository.AccountPendingChanges

class AccountPendingStore(private val db: FormLinkDatabase) {
    fun observe(owner: String) = db.drafts().observeAccountPending(owner)
    suspend fun snapshot(owner: String): AccountPendingChanges = db.withTransaction {
        val forms = db.drafts().owned(owner).filter { it.form.revision != it.form.syncedRevision }
        AccountPendingChanges(forms.count { !it.form.deleted }, forms.count { it.form.deleted },
            db.cloud().requests(owner).count { it.statusRevision != it.syncedStatusRevision })
    }
}
