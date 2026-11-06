package com.uvg.cc3087.myapp.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Versioned authenticated encryption. Keys never leave Android Keystore. */
class DeviceCipher(context: Context) {
    private val directory = context.noBackupFilesDir
    private val key: SecretKey get() = synchronized(lock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
    }
    fun encrypt(bytes: ByteArray): ByteArray = Cipher.getInstance("AES/GCM/NoPadding").run {
        init(Cipher.ENCRYPT_MODE, key)
        byteArrayOf(1) + iv + doFinal(bytes)
    }
    fun decrypt(bytes: ByteArray): ByteArray {
        require(bytes.size >= 29 && bytes[0] == 1.toByte()) { "Invalid encrypted data" }
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            doFinal(bytes.copyOfRange(13, bytes.size))
        }
    }
    @Synchronized fun read(name: String): ByteArray? {
        require(name.matches(Regex("[a-z-]+")))
        val file = AtomicFile(File(directory, name))
        return if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) decrypt(file.readFully()) else null
    }
    @Synchronized fun write(name: String, bytes: ByteArray) {
        require(name.matches(Regex("[a-z-]+")))
        val file = AtomicFile(File(directory, name))
        val output = file.startWrite()
        try { output.write(encrypt(bytes)); file.finishWrite(output) }
        catch (error: Throwable) { file.failWrite(output); throw error }
    }
    fun remove(name: String) { require(name.matches(Regex("[a-z-]+"))); AtomicFile(File(directory, name)).delete() }
    companion object { private const val ALIAS = "formlink.device.v1"; private val lock = Any() }
}
