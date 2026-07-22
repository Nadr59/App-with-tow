package com.apppair.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_pair_prefs")

@Singleton
class AppPairPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_APP1 = stringPreferencesKey("app1_package")
        val KEY_APP2 = stringPreferencesKey("app2_package")
        val KEY_SERVICE_ACTIVE = booleanPreferencesKey("service_active")
    }

    val app1Package: Flow<String?> = context.dataStore.data.map { it[KEY_APP1] }
    val app2Package: Flow<String?> = context.dataStore.data.map { it[KEY_APP2] }
    val serviceActive: Flow<Boolean> = context.dataStore.data.map { it[KEY_SERVICE_ACTIVE] ?: false }

    suspend fun saveSelectedApps(pkgA: String, pkgB: String) {
        context.dataStore.edit { it[KEY_APP1] = pkgA; it[KEY_APP2] = pkgB }
    }

    suspend fun setServiceActive(active: Boolean) {
        context.dataStore.edit { it[KEY_SERVICE_ACTIVE] = active }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(KEY_APP1); it.remove(KEY_APP2); it.remove(KEY_SERVICE_ACTIVE) }
    }
}
