package com.uvg.cc3087.myapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.uvg.cc3087.myapp.data.model.ThemeMode
import com.uvg.cc3087.myapp.data.repository.AppearanceRepository
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance")

class DataStoreAppearanceRepository(private val store: DataStore<Preferences>) : AppearanceRepository {
    override val themeMode = store.data.map { ThemeMode.fromStored(it[THEME_MODE]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        fun create(context: Context) = DataStoreAppearanceRepository(context.applicationContext.appearanceDataStore)
    }
}
