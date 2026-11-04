package com.uvg.cc3087.myapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FormDraftEntity::class, FormFieldEntity::class, RequestEntity::class], version = 3, exportSchema = true)
@TypeConverters(FieldOptionConverters::class)
abstract class FormLinkDatabase : RoomDatabase() {
    abstract fun drafts(): FormDraftDao
    abstract fun requests(): RequestDao

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
        fun open(context: Context): FormLinkDatabase = Room.databaseBuilder(
            context.applicationContext,
            FormLinkDatabase::class.java,
            "formlink-drafts.db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        // no usamos migraciones destructivas para resolver cambios futuros borrando formularios
    }
}
