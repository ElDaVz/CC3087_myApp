package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.local.OwnerProfileDao
import com.uvg.cc3087.myapp.data.local.PrivateLogoStorage
import com.uvg.cc3087.myapp.data.local.toEntity
import com.uvg.cc3087.myapp.data.local.toModel
import com.uvg.cc3087.myapp.data.model.ContactEmail
import com.uvg.cc3087.myapp.data.model.OwnerProfile
import com.uvg.cc3087.myapp.data.remote.http.FirestoreRest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Contact data is owner-scoped. Compose never touches Room or Firestore directly. */
interface ProfileRepository {
    fun observe(owner: String): Flow<OwnerProfile>
    suspend fun get(owner: String): OwnerProfile = observe(owner).first()
    suspend fun save(owner: String, profile: OwnerProfile)
    suspend fun importLogo(owner: String, source: String)
    suspend fun removeLogo(owner: String)
    /** Flushes pending local profile revisions to the private owner document. */
    suspend fun sync(owner: String)
}

class RoomProfileRepository(
    private val database: FormLinkDatabase,
    private val logos: PrivateLogoStorage,
    private val client: FirestoreRest? = null,
    private val currentOwner: () -> String? = { null }
) : ProfileRepository {
    private val dao: OwnerProfileDao get() = database.profiles()

    override fun observe(owner: String): Flow<OwnerProfile> =
        dao.observe(owner).map { it?.toModel() ?: OwnerProfile(owner) }

    override suspend fun save(owner: String, profile: OwnerProfile) {
        check(currentOwner() == null || currentOwner() == owner) { "Profile is outside this session" }
        require(profile.ownerId == owner)
        require(profile.personalName.length <= 100 && profile.businessName.length <= 100)
        require(profile.personalEmail.length <= 120 && profile.businessEmail.length <= 120)
        listOf(profile.personalEmail, profile.businessEmail).forEach {
            require(ContactEmail.isValid(it)) { "Invalid contact email" }
        }
        val previous = dao.get(owner)
        val stored = profile.copy(
            revision = (previous?.revision ?: 0) + 1,
            syncedRevision = previous?.syncedRevision ?: 0
        )
        dao.save(stored.toEntity())
        val oldLogo = previous?.logoPath?.takeIf { it != stored.logoPath }
        oldLogo?.let { logos.delete(it) }
    }

    override suspend fun importLogo(owner: String, source: String) {
        check(currentOwner() == null || currentOwner() == owner) { "Profile is outside this session" }
        val path = logos.importImage(source)
        try {
            val current = dao.get(owner)?.toModel() ?: OwnerProfile(owner)
            save(owner, current.copy(logoPath = path))
        } catch (error: Exception) {
            logos.delete(path)
            throw error
        }
    }

    override suspend fun removeLogo(owner: String) {
        val current = dao.get(owner)?.toModel() ?: return
        save(owner, current.copy(logoPath = null))
    }

    override suspend fun sync(owner: String) {
        val client = client ?: return
        check(currentOwner() == owner) { "Profile is outside this session" }
        val row = dao.get(owner) ?: return
        if (row.revision == row.syncedRevision) return
        client.commit(owner, listOf(client.update("profiles/$owner", mapOf(
            "ownerId" to owner,
            "personalName" to row.personalName,
            "personalEmail" to row.personalEmail,
            "businessName" to row.businessName,
            "businessEmail" to row.businessEmail
        ))))
        // Only a confirmed write may be acknowledged; a late retry keeps the revision.
        dao.acknowledge(owner, row.revision)
    }
}
