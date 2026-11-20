package com.uvg.cc3087.myapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FormDraftEntity::class, FormFieldEntity::class, RequestEntity::class, PublicationEntity::class, RemoteRequestEntity::class, AnalyticsEventEntity::class, CoverUploadEntity::class, RequestReadEntity::class, GuestImportEntity::class, OwnerProfileEntity::class, FormLinkEntity::class], version = 11, exportSchema = true)
@TypeConverters(FieldOptionConverters::class)
abstract class FormLinkDatabase : RoomDatabase() {
    abstract fun guestImports(): GuestImportDao
    abstract fun requestReads(): RequestReadDao
    abstract fun drafts(): FormDraftDao
    abstract fun requests(): RequestDao
    abstract fun cloud(): CloudDao
    abstract fun analytics(): AnalyticsDao
    abstract fun profiles(): OwnerProfileDao
    abstract fun links(): LinkDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `local_requests` (
                    `formId` TEXT NOT NULL, `responseId` TEXT NOT NULL,
                    `formTitle` TEXT NOT NULL, `submittedAt` INTEGER NOT NULL,
                    `status` TEXT NOT NULL, `answersJson` TEXT NOT NULL,
                    PRIMARY KEY(`formId`, `responseId`))""")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN coverPath TEXT")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN ownerId TEXT")
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN syncedRevision INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN creationAttemptId TEXT")
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN creationStartedAt INTEGER")
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN templateType TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS form_publications (formId TEXT NOT NULL PRIMARY KEY, ownerId TEXT NOT NULL, status TEXT NOT NULL, url TEXT NOT NULL, publishedAt INTEGER NOT NULL, version INTEGER NOT NULL, fingerprint TEXT NOT NULL, firstPublishedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS remote_requests (ownerId TEXT NOT NULL, formId TEXT NOT NULL, responseId TEXT NOT NULL, formTitle TEXT NOT NULL, submittedAt INTEGER NOT NULL, status TEXT NOT NULL, answersJson TEXT NOT NULL, statusRevision INTEGER NOT NULL DEFAULT 0, syncedStatusRevision INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(ownerId, formId, responseId))")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS analytics_events (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, parametersJson TEXT NOT NULL, dispatched INTEGER NOT NULL DEFAULT 0)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE form_drafts ADD COLUMN coverAction TEXT NOT NULL DEFAULT 'KEEP'")
                db.execSQL("ALTER TABLE form_publications ADD COLUMN coverUrl TEXT")
                db.execSQL("ALTER TABLE form_publications ADD COLUMN coverPublicId TEXT")
                db.execSQL("ALTER TABLE form_publications ADD COLUMN coverHash TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS cover_uploads (ownerId TEXT NOT NULL, formId TEXT NOT NULL, hash TEXT NOT NULL, url TEXT NOT NULL, publicId TEXT NOT NULL, PRIMARY KEY(ownerId, formId, hash))")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS request_reads (scope TEXT NOT NULL, formId TEXT NOT NULL, responseId TEXT NOT NULL, PRIMARY KEY(scope, formId, responseId))")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS guest_imports (formId TEXT NOT NULL PRIMARY KEY, ownerId TEXT NOT NULL, completed INTEGER NOT NULL)")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS owner_profiles (
                    `ownerId` TEXT NOT NULL PRIMARY KEY, `personalName` TEXT NOT NULL DEFAULT '',
                    `personalEmail` TEXT NOT NULL DEFAULT '', `businessName` TEXT NOT NULL DEFAULT '',
                    `businessEmail` TEXT NOT NULL DEFAULT '', `logoPath` TEXT,
                    `revision` INTEGER NOT NULL DEFAULT 0, `syncedRevision` INTEGER NOT NULL DEFAULT 0)""")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS form_links (
                    `ownerId` TEXT NOT NULL, `formId` TEXT NOT NULL,
                    `singleUseToken` TEXT, `trackingToken` TEXT NOT NULL,
                    PRIMARY KEY(`ownerId`, `formId`))""")
            }
        }
        fun open(context: Context): FormLinkDatabase = Room.databaseBuilder(
            context.applicationContext,
            FormLinkDatabase::class.java,
            "formlink-drafts.db"
        ).openHelperFactory(com.uvg.cc3087.myapp.data.security.EncryptedDatabase.factory(context, "formlink-drafts.db"))
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11).build()
        // no usamos migraciones destructivas para resolver cambios futuros borrando formularios
    }
}
