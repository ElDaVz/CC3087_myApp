package com.uvg.cc3087.myapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [FormDraftEntity::class, FormFieldEntity::class], version = 1, exportSchema = true)
@TypeConverters(FieldOptionConverters::class)
abstract class FormLinkDatabase : RoomDatabase() {
    abstract fun drafts(): FormDraftDao

    companion object {
        fun open(context: Context): FormLinkDatabase = Room.databaseBuilder(
            context.applicationContext,
            FormLinkDatabase::class.java,
            "formlink-drafts.db"
        ).build()
        // no usamos migraciones destructivas para resolver cambios futuros borrando formularios
    }
}
