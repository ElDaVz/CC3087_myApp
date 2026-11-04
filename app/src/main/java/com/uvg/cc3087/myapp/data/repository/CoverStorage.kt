package com.uvg.cc3087.myapp.data.repository

interface CoverStorage {
    suspend fun importImage(source: String): String
    suspend fun delete(path: String)
}
