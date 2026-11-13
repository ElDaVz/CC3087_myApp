package com.uvg.cc3087.myapp.data.local

import androidx.room.withTransaction
import com.uvg.cc3087.myapp.data.repository.GuestImportCounts
import com.uvg.cc3087.myapp.data.repository.GuestImportStore
import kotlinx.coroutines.flow.map

class RoomGuestImportStore(private val database: FormLinkDatabase) : GuestImportStore {
    private val imports get() = database.guestImports()
    override suspend fun snapshotGuestIds() = imports.availableGuestIds().toSet()
    override fun observe(owner: String) = imports.observe(owner).map { rows ->
        GuestImportCounts(rows.count { !it.completed }, rows.count { it.completed })
    }
    override suspend fun prepare(owner: String, ids: Set<String>) = database.withTransaction {
        require(owner.isNotBlank())
        for (id in ids) {
            val row = database.drafts().getDraft(id)?.form ?: continue
            if (row.ownerId == null && !row.deleted) imports.reserve(GuestImportEntity(id, owner))
        }
    }
    override suspend fun transfer(owner: String): Int = database.withTransaction {
        var moved = 0
        for (entry in imports.pending(owner)) {
            val row = database.drafts().getDraft(entry.formId)?.form
            if (row == null || row.deleted) {
                imports.discard(owner, entry.formId)
                continue
            }
            check(row.ownerId == null || row.ownerId == owner) { "Draft belongs to another account" }
            if (row.ownerId == null) moved += database.drafts().claimGuest(owner, entry.formId)
            imports.complete(owner, entry.formId)
        }
        moved
    }
}
