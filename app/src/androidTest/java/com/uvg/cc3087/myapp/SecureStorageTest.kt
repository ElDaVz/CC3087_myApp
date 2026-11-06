package com.uvg.cc3087.myapp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.*
import com.uvg.cc3087.myapp.data.model.FormDraft
import com.uvg.cc3087.myapp.data.security.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class SecureStorageTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun ciphertextHasFreshNonceAndRejectsTampering() {
        val cipher = DeviceCipher(context)
        val plain = "sensitive-refresh-token".toByteArray()
        val first = cipher.encrypt(plain)
        assertFalse(first.contentEquals(cipher.encrypt(plain)))
        assertArrayEquals(plain, cipher.decrypt(first))
        first[first.lastIndex] = (first.last().toInt() xor 1).toByte()
        assertTrue(runCatching { cipher.decrypt(first) }.isFailure)
    }
    @Test fun plaintextDatabaseMigratesWithoutLosingPendingGuestDrafts() = runBlocking {
        val name = "secure-${UUID.randomUUID()}.db"
        try {
            Room.databaseBuilder(context, FormLinkDatabase::class.java, name).build().let { db ->
                try { RoomFormStore(db).save(null, FormDraft("f", "SECRET DOMAIN DATA", emptyList(), 1, 2)) }
                finally { db.close() }
            }
            repeat(2) {
                val db = Room.databaseBuilder(context, FormLinkDatabase::class.java, name)
                    .openHelperFactory(EncryptedDatabase.factory(context, name)).build()
                try { assertEquals("SECRET DOMAIN DATA", RoomFormStore(db).get(null, "f")!!.title) }
                finally { db.close() }
                assertFalse(String(context.getDatabasePath(name).readBytes(), Charsets.ISO_8859_1).contains("SECRET DOMAIN DATA"))
                // Android's default corruption handler deletes an unreadable database: probe a copy.
                val probe = java.io.File(context.cacheDir, "$name.probe")
                probe.writeBytes(context.getDatabasePath(name).readBytes())
                try { assertTrue(runCatching { android.database.sqlite.SQLiteDatabase.openDatabase(probe.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { plain -> plain.rawQuery("SELECT * FROM form_drafts", null).use { it.moveToFirst() } } }.isFailure) }
                finally { probe.delete() }
            }
        } finally { context.deleteDatabase(name) }
    }
}
