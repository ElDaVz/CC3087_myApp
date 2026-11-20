package com.uvg.cc3087.myapp.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.uvg.cc3087.myapp.data.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAppearanceRepositoryTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun preferenceSurvivesClosingAndReopeningTheStore() = runBlocking {
        val file = directory.newFolder().resolve("appearance.preferences_pb")
        val firstJob = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + firstJob)) { file }
        val repository = DataStoreAppearanceRepository(store)
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
        firstJob.cancelAndJoin()
        val secondJob = SupervisorJob()
        try {
            val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + secondJob)) { file }
            assertEquals(ThemeMode.DARK, DataStoreAppearanceRepository(reopened).themeMode.first())
            reopened.edit { it[stringPreferencesKey("theme_mode")] = "future-mode" }
            assertEquals(ThemeMode.SYSTEM, DataStoreAppearanceRepository(reopened).themeMode.first())
        } finally {
            secondJob.cancelAndJoin()
        }
    }
}
