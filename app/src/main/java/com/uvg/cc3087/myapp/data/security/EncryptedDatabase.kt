package com.uvg.cc3087.myapp.data.security

import android.content.Context
import java.security.SecureRandom
import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** Export first, verify, then atomically replace. An interrupted export leaves the original intact. */
object EncryptedDatabase {
    @Synchronized fun factory(context: Context, name: String): SupportOpenHelperFactory {
        System.loadLibrary("sqlcipher")
        val cipher = DeviceCipher(context)
        val passphrase = cipher.read("database-key") ?: ByteArray(32).also {
            SecureRandom().nextBytes(it); cipher.write("database-key", it)
        }
        val password = passphrase.joinToString("") { "%02x".format(it) }.toByteArray()
        val file = context.getDatabasePath(name)
        if (file.exists() && file.inputStream().use { String(ByteArray(16).also { header -> it.read(header) }) }.startsWith("SQLite format 3")) {
            val target = File(file.path + ".encrypted")
            if (target.exists()) check(target.delete())
            val plain = SQLiteDatabase.openDatabase(file.path, "", null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY, null)
            try {
                plain.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                val version = plain.rawQuery("PRAGMA user_version", null).use { it.moveToFirst(); it.getInt(0) }
                plain.execSQL("ATTACH DATABASE ? AS encrypted KEY ?", arrayOf(target.path, String(password)))
                plain.rawQuery("SELECT sqlcipher_export('encrypted')", null).use { it.moveToFirst() }
                plain.execSQL("PRAGMA encrypted.user_version = $version")
                plain.execSQL("DETACH DATABASE encrypted")
            } finally { plain.close() }
            val encrypted = SQLiteDatabase.openDatabase(target.path, password, null, SQLiteDatabase.OPEN_READWRITE, null)
            try { encrypted.rawQuery("PRAGMA integrity_check", null).use { check(it.moveToFirst() && it.getString(0) == "ok") } }
            finally { encrypted.close() }
            check(target.renameTo(file)) { "Cannot replace migrated database" }
            File(file.path + "-wal").delete(); File(file.path + "-shm").delete()
        }
        return SupportOpenHelperFactory(password)
    }
}
