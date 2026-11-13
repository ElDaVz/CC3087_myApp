package com.uvg.cc3087.myapp

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.*
import com.uvg.cc3087.myapp.data.remote.*
import com.uvg.cc3087.myapp.data.remote.http.*
import com.uvg.cc3087.myapp.data.repository.*
import com.uvg.cc3087.myapp.data.security.DeviceCipher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

class CloudinaryPublicationTest {
    @Test fun coverRetryRemovalAndOtherDeviceEditingPreservePublishedData() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backendEmulators") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val auth = RestAuthRepository("demo-api-key", DeviceCipher(context), FirebaseHttp(true), true)
        auth.state.first { it !is AuthState.Initializing }; auth.signOut()
        val authEmail = "cover-${UUID.randomUUID()}@example.com"
        auth.signUp(authEmail, "password123")
        val owner = auth.state.value.userId!!
        var currentOwner: String? = owner
        val client = FirestoreRest("demo-formlink", auth, FirebaseHttp(true), true)
        val source = RestFormSource(client)
        val db = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
        val bytes = byteArrayOf(1, 2, 3)
        val hash = "a".repeat(64)
        val image = CoverAsset("https://res.cloudinary.com/uxrkjzti/image/upload/v123/test-cover.jpg", "test-cover", hash)
        val covers = object : CoverStorage {
            override suspend fun importImage(source: String) = error("unused")
            override suspend fun delete(path: String) = Unit
            override suspend fun prepareUpload(path: String) = PreparedCover(bytes, hash)
        }
        val local = RoomFormStore(db)
        var uploadCount = 0
        var changeDuringUpload = true
        var failUpload = false
        val id = "cover-${UUID.randomUUID()}"
        val base = FormDraft(id, "Portada", listOf(FormFieldDraft("q", FormFieldType.TEXT, "Pregunta")), 1, 1, ownerId = owner)
        val withCover = base.copy(coverPath = "covers/fixture.jpg", coverAction = "REPLACE")
        val uploader = CoverUploader {
            uploadCount++
            if (failUpload) throw PublicationException(com.uvg.cc3087.myapp.data.repository.PublicationError.COVER_UPLOAD_FAILED)
            if (changeDuringUpload) { changeDuringUpload = false; local.save(owner, withCover.copy(title = "Edición posterior")) }
            image
        }
        val publisher = RestFormPublisher(client, source, db, { currentOwner }, { it() }, covers = covers, uploader = uploader)
        try {
            local.save(owner, base)
            val first = publisher.publish(owner, base)
            local.save(owner, withCover)
            // A successful upload followed by a rejected stale publication must be reusable.
            assertTrue(runCatching { publisher.publish(owner, withCover) }.isFailure)
            assertEquals(first.version, (FirestoreRest.fields(client.get(owner, "publishedForms/$id")!!)["version"] as Number).toLong())
            local.save(owner, withCover)
            val second = publisher.publish(owner, withCover)
            assertEquals(1, uploadCount)
            assertEquals(image.url, second.coverUrl)
            // Sign in on an installation with no local originals or publication cache.
            auth.signOut()
            auth.signIn(authEmail, "password123")
            val recoveredDb = Room.inMemoryDatabaseBuilder(context, FormLinkDatabase::class.java).build()
            try {
                val recovered = RoomFormStore(recoveredDb)
                recovered.mergeRemote(owner, source.fetch(owner))
                assertNull(recovered.get(owner, id)!!.coverPath)
                assertNull(recovered.observe(owner).first().single { it.id == id }.publishedCoverUrl)
                RestFormPublisher(client, source, recoveredDb, { currentOwner }, { it() }).refresh(owner)
                assertEquals(image.url, recovered.observe(owner).first().single { it.id == id }.publishedCoverUrl)
                assertEquals(image.url, recovered.observePublishedCoverUrl(owner, id).first())
                assertEquals(1, uploadCount)
            } finally { recoveredDb.close() }
            assertEquals(second.version, publisher.publish(owner, withCover).version)
            assertTrue(publisher.publication(owner, withCover)!!.matchesDraft)
            assertFalse(publisher.publication(owner, withCover.copy(coverAction = "REMOVE"))!!.matchesDraft)
            // A different device has the form but no private cover file.
            val otherDevice = base.copy(title = "Editado sin archivo local")
            local.save(owner, otherDevice)
            val third = publisher.publish(owner, otherDevice)
            assertEquals(image.url, third.coverUrl)
            assertEquals(1, uploadCount)
            // Failed replacement preserves the old public cover.
            val replacement = withCover.copy(title = "Reemplazo", coverPath = "covers/new.jpg")
            db.clearAllTables()
            local.save(owner, replacement)
            failUpload = true
            assertTrue(runCatching { publisher.publish(owner, replacement) }.isFailure)
            assertEquals(image.url, (FirestoreRest.fields(client.get(owner, "publishedForms/$id")!!)["cover"] as Map<*, *>)["url"])
            failUpload = false
            val removed = otherDevice.copy(coverAction = "REMOVE")
            local.save(owner, removed)
            val fourth = publisher.publish(owner, removed)
            assertNull(fourth.coverUrl)
            assertNull(FirestoreRest.fields(client.get(owner, "publishedForms/$id")!!)["cover"])
            // Deletion is retryable and stale devices cannot restore the publication.
            source.delete(owner, id)
            source.delete(owner, id)
            assertEquals(true, FirestoreRest.fields(client.get(owner, "forms/$id")!!)["deleted"])
            assertEquals(false, FirestoreRest.fields(client.get(owner, "publishedForms/$id")!!)["active"])
            source.put(owner, removed.copy(title = "Stale edit"))
            assertTrue(source.fetch(owner).none { it.id == id })
            assertTrue(runCatching { publisher.publish(owner, removed) }.isFailure)
            val engine = FormSyncEngine(local, source) { currentOwner }
            assertEquals(SyncResult.SUCCESS, engine.sync(owner))
            assertNull(local.get(owner, id))
            // An unpublished draft can be removed repeatedly as well.
            val unpublished = base.copy(id = "draft-${UUID.randomUUID()}")
            source.put(owner, unpublished)
            source.delete(owner, unpublished.id)
            source.delete(owner, unpublished.id)
            assertEquals(true, FirestoreRest.fields(client.get(owner, "forms/${unpublished.id}")!!)["deleted"])
            source.put(owner, unpublished.copy(title = "Stale unpublished edit"))
            assertTrue(source.fetch(owner).none { it.id == unpublished.id })
            currentOwner = "another-owner"
            assertTrue(runCatching { publisher.publish(owner, removed) }.isFailure)
        } finally { db.close(); auth.signOut() }
    }

    @Test fun realUnsignedUploadOfASyntheticImage() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("realCloudinary") == "true")
        val bitmap = Bitmap.createBitmap(48, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(102, 80, 164)) }
        val bytes = java.io.ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
        bitmap.recycle()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val result = CloudinaryCoverUploader(BuildConfig.CLOUDINARY_CLOUD_NAME, BuildConfig.CLOUDINARY_UPLOAD_PRESET).upload(PreparedCover(bytes, hash))
        assertTrue(CoverAsset.allowedUrl(result.url))
        assertEquals(hash, result.hash)
    }
}
