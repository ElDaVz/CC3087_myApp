package com.uvg.cc3087.myapp

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.uvg.cc3087.myapp.data.local.FormLinkDatabase
import com.uvg.cc3087.myapp.data.repository.RoomFormRepository
import com.uvg.cc3087.myapp.data.repository.RoomRequestRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class RoomMigrationTest {
    @Test fun versionTwoAndThreeKeepDraftsRequestsAndOptionalCovers() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        for (version in listOf(2, 3, 6)) {
            val name = "migration-$version-${UUID.randomUUID()}.db"
            try {
                val schema = assets.open("com.uvg.cc3087.myapp.data.local.FormLinkDatabase/$version.json")
                    .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
                context.getDatabasePath(name).parentFile!!.mkdirs()
                SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { old ->
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        val table = entity.getString("tableName")
                        old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                        for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
                    old.execSQL("INSERT INTO form_drafts (id,title,createdAt,updatedAt) VALUES ('old','Conservar',1,2)")
                    if (version >= 3) old.execSQL("UPDATE form_drafts SET coverPath='covers/old.jpg' WHERE id='old'")
                    old.execSQL("INSERT INTO local_requests VALUES ('old','response','Snapshot',2,'PENDING','[]')")
                    if (version == 6) {
                        old.execSQL("INSERT INTO form_publications VALUES ('old','owner','PUBLISHED','https://example.test/f/old',2,1,'hash',2)")
                        old.execSQL("INSERT INTO analytics_events VALUES ('event','form_creation_started','{}',0)")
                    }
                    old.version = version
                }
                val db = Room.databaseBuilder(context, FormLinkDatabase::class.java, name)
                    .addMigrations(FormLinkDatabase.MIGRATION_1_2, FormLinkDatabase.MIGRATION_2_3, FormLinkDatabase.MIGRATION_3_4, FormLinkDatabase.MIGRATION_4_5, FormLinkDatabase.MIGRATION_5_6, FormLinkDatabase.MIGRATION_6_7).build()
                try {
                    val draft = RoomFormRepository(db.drafts()).getDraft("old")!!
                    assertEquals("Conservar", draft.title)
                    assertEquals("KEEP", draft.coverAction)
                    assertNull(draft.ownerId)
                    if (version == 6) {
                        assertEquals("hash", db.cloud().publication("owner", "old")!!.fingerprint)
                        assertNull(db.cloud().publication("owner", "old")!!.coverUrl)
                        assertEquals(1, db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM analytics_events").use { it.moveToFirst(); it.getInt(0) })
                    }
                    assertEquals(if (version >= 3) "covers/old.jpg" else null, draft.coverPath)
                    assertEquals("Snapshot", RoomRequestRepository(db).getDetail("old", "response")!!.formTitle)
                } finally { db.close() }
            } finally { context.deleteDatabase(name) }
        }
    }
}
