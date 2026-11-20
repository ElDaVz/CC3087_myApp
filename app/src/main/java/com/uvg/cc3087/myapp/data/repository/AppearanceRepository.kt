package com.uvg.cc3087.myapp.data.repository

import com.uvg.cc3087.myapp.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Device preference, deliberately independent of authenticated sessions. */
interface AppearanceRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
