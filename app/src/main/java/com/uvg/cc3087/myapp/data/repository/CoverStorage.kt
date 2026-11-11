package com.uvg.cc3087.myapp.data.repository

data class PreparedCover(val bytes: ByteArray, val hash: String)

interface CoverStorage {
    suspend fun prepareUpload(path: String): PreparedCover = error("Cover upload is unavailable")
    suspend fun importImage(source: String): String
    suspend fun delete(path: String)
}
