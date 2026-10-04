package com.uvg.cc3087.myapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class AppPreferencesManager(private val context: Context) {

    companion object {
        private val KEY_LAST_EDITED_FORM_ID = stringPreferencesKey("last_edited_form_id")
        private val KEY_ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        private val KEY_ACTIVE_USER_EMAIL = stringPreferencesKey("active_user_email")
        private val KEY_ACTIVE_USER_NAME = stringPreferencesKey("active_user_name")
        private val KEY_AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
    }

    val lastEditedFormId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_EDITED_FORM_ID]
    }

    suspend fun saveLastEditedFormId(formId: String?) {
        context.dataStore.edit { prefs ->
            if (formId == null) {
                prefs.remove(KEY_LAST_EDITED_FORM_ID)
            } else {
                prefs[KEY_LAST_EDITED_FORM_ID] = formId
            }
        }
    }

    val activeUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_USER_ID]
    }

    val activeUserEmail: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_USER_EMAIL]
    }

    val activeUserName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_USER_NAME]
    }

    suspend fun saveUserSession(userId: String?, email: String?, name: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) {
                prefs.remove(KEY_ACTIVE_USER_ID)
                prefs.remove(KEY_ACTIVE_USER_EMAIL)
                prefs.remove(KEY_ACTIVE_USER_NAME)
            } else {
                prefs[KEY_ACTIVE_USER_ID] = userId
                if (email != null) prefs[KEY_ACTIVE_USER_EMAIL] = email
                if (name != null) prefs[KEY_ACTIVE_USER_NAME] = name
            }
        }
    }

    val autoSaveEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SAVE_ENABLED] ?: true
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SAVE_ENABLED] = enabled
        }
    }
}
