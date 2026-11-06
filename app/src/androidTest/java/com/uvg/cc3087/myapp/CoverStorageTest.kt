package com.uvg.cc3087.myapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.repository.RoomFormRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class CoverStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun importedCoverAndBlurSurviveRoomReopenAndCanBeRemoved() = runBlocking {
        val source = File(context.cacheDir, "source-${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        ExifInterface(source.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString()); saveAttributes()
        }
        val storage = PrivateCoverStorage(context)
        val name = "cover-${UUID.randomUUID()}.db"
        var path: String? = null
        try {
            path = storage.importImage(android.net.Uri.fromFile(source).toString())
            assertTrue(File(context.filesDir, path + ".blur.jpg").exists())
            val bytes = com.uvg.cc3087.myapp.data.security.DeviceCipher(context).decrypt(File(context.filesDir, path).readBytes())
            val normalized = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertEquals(40, normalized.width); assertEquals(80, normalized.height); normalized.recycle()
            var database = Room.databaseBuilder(context, FormLinkDatabase::class.java, name).build()
            try { RoomFormRepository(database.drafts(), covers = storage).saveDraft(FormDraft("f", "Cover", emptyList(), 1, 1, path)) }
            finally { database.close() }
            database = Room.databaseBuilder(context, FormLinkDatabase::class.java, name).build()
            try {
                val repository = RoomFormRepository(database.drafts(), covers = storage)
                val saved = repository.getDraft("f")!!
                assertEquals(path, saved.coverPath)
                assertTrue(runCatching { storage.importImage(android.net.Uri.fromFile(File(context.cacheDir, "missing-image")).toString()) }.isFailure)
                assertEquals(path, repository.getDraft("f")!!.coverPath)
                repository.saveDraft(saved.copy(coverPath = null))
                assertFalse(File(context.filesDir, path).exists())
                assertFalse(File(context.filesDir, path + ".blur.jpg").exists())
            } finally { database.close() }
        } finally {
            source.delete(); context.deleteDatabase(name)
            path?.let { storage.delete(it) }
        }
    }
}
