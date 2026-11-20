package com.uvg.cc3087.myapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.uvg.cc3087.myapp.data.repository.FormListPreferences
import com.uvg.cc3087.myapp.ui.state.FormSortOrder
import kotlinx.coroutines.flow.map

private val Context.formListDataStore by preferencesDataStore(name = "form_list_preferences")

/** Sort preference is device-local and partitioned by guest/account, never synchronized. */
class DataStoreFormListPreferences(private val store: DataStore<Preferences>) : FormListPreferences {
    override fun observe(owner: String) = store.data.map { preferences ->
        FormSortOrder.entries.firstOrNull {
            it.name == preferences[stringPreferencesKey("sort_order:$owner")]
        } ?: FormSortOrder.RECENT_FIRST
    }

    override suspend fun set(owner: String, order: FormSortOrder) {
        store.edit { it[stringPreferencesKey("sort_order:$owner")] = order.name }
    }

    companion object {
        fun create(context: Context) = DataStoreFormListPreferences(
            context.applicationContext.formListDataStore
        )
    }
}
