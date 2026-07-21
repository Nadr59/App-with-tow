package com.apppair.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore extension على مستوى الملف
private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "app_pair_prefs")

@Singleton
class AppPairPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val APP1_PACKAGE = stringPreferencesKey("app1_package")
        private val APP2_PACKAGE = stringPreferencesKey("app2_package")
        private val SAVED_TASK_IDS = stringSetPreferencesKey("saved_task_ids")
    }

    // ── حفظ التطبيقات المختارة ──
    val app1Package: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[APP1_PACKAGE]
    }

    val app2Package: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[APP2_PACKAGE]
    }

    suspend fun saveSelectedApps(package1: String, package2: String) {
        context.dataStore.edit { prefs ->
            prefs[APP1_PACKAGE] = package1
            prefs[APP2_PACKAGE] = package2
        }
    }

    suspend fun saveTaskId(packageName: String, taskId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[SAVED_TASK_IDS]?.toMutableSet() ?: mutableSetOf()
            current.add("$packageName:$taskId")
            prefs[SAVED_TASK_IDS] = current
        }
    }

    suspend fun clearSelection() {
        context.dataStore.edit { prefs ->
            prefs.remove(APP1_PACKAGE)
            prefs.remove(APP2_PACKAGE)
            prefs.remove(SAVED_TASK_IDS)
        }
    }
}
