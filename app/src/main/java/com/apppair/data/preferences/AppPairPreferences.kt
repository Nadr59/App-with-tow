package com.apppair.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apppair.data.model.SelectedAppPair
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "apppair_prefs")

@Singleton
class AppPairPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_PACKAGE_A = stringPreferencesKey("selected_package_a")
        private val KEY_PACKAGE_B = stringPreferencesKey("selected_package_b")
        private val KEY_SERVICE_ACTIVE = booleanPreferencesKey("service_active")
        private val KEY_WIDGET_X = intPreferencesKey("widget_x")
        private val KEY_WIDGET_Y = intPreferencesKey("widget_y")
        private val KEY_WIDGET_MINIMIZED = booleanPreferencesKey("widget_minimized")
    }

    val selectedAppPairFlow: Flow<SelectedAppPair> = context.dataStore.data.map { preferences ->
        SelectedAppPair(
            packageA = preferences[KEY_PACKAGE_A],
            packageB = preferences[KEY_PACKAGE_B],
            isServiceActive = preferences[KEY_SERVICE_ACTIVE] ?: false,
            widgetX = preferences[KEY_WIDGET_X] ?: 100,
            widgetY = preferences[KEY_WIDGET_Y] ?: 300,
            isMinimized = preferences[KEY_WIDGET_MINIMIZED] ?: false
        )
    }

    suspend fun saveSelectedApps(packageA: String, packageB: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PACKAGE_A] = packageA
            preferences[KEY_PACKAGE_B] = packageB
        }
    }

    suspend fun setServiceActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVICE_ACTIVE] = active
        }
    }

    suspend fun saveWidgetPosition(x: Int, y: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WIDGET_X] = x
            preferences[KEY_WIDGET_Y] = y
        }
    }

    suspend fun setWidgetMinimized(minimized: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WIDGET_MINIMIZED] = minimized
        }
    }

    suspend fun clearSelectedApps() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PACKAGE_A)
            preferences.remove(KEY_PACKAGE_B)
            preferences[KEY_SERVICE_ACTIVE] = false
        }
    }
}
