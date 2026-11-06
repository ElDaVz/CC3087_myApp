package com.uvg.cc3087.myapp.data.security

import android.content.Context
import android.util.AtomicFile
import java.io.File

object CoverEncryptionMigration {
    fun migrate(context: Context) {
        val cipher = DeviceCipher(context)
        File(context.filesDir, "covers").listFiles()?.filter { it.name.endsWith(".jpg") }?.forEach { file ->
            val bytes = AtomicFile(file).readFully()
            if (bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte()) {
                val atomic = AtomicFile(file)
                val encrypted = cipher.encrypt(bytes)
                check(cipher.decrypt(encrypted).contentEquals(bytes))
                val output = atomic.startWrite()
                try { output.write(encrypted); atomic.finishWrite(output) }
                catch (error: Throwable) { atomic.failWrite(output); throw error }
            }
        }
        // Old imports could have been interrupted before the previous release's finally block.
        File(context.filesDir, "covers").listFiles()?.filter { it.name.startsWith("import-") && it.name.endsWith(".tmp") }?.forEach { it.delete() }
    }
}
