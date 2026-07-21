#!/bin/bash
# ══════════════════════════════════════════════════════════════
# setup-app-pair.sh
# يُنشئ مشروع AppPair بالكامل
# طريقة التشغيل: bash setup-app-pair.sh
# ══════════════════════════════════════════════════════════════

set -e

PROJECT_ROOT="app"
SRC="app/src/main/java/com/apppair"
RES="app/src/main/res"

echo ">>> Creating directory structure..."

mkdir -p "$SRC/data/model"
mkdir -p "$SRC/data/preferences"
mkdir -p "$SRC/data/repository"
mkdir -p "$SRC/utils"
mkdir -p "$SRC/service"
mkdir -p "$SRC/ui/theme"
mkdir -p "$RES/values"
mkdir -p "$RES/values-night"
mkdir -p "$RES/drawable"
mkdir -p "$RES/mipmap-anydpi-v26"
mkdir -p "gradle/wrapper"
mkdir -p ".github/workflows"

echo ">>> Writing all project files..."

# ═══════════════════════════════════════════════
# build.gradle.kts (root)
# ═══════════════════════════════════════════════
cat > build.gradle.kts << 'FILEEOF'
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
}
FILEEOF

# ═══════════════════════════════════════════════
# settings.gradle.kts
# ═══════════════════════════════════════════════
cat > settings.gradle.kts << 'FILEEOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppPair"
include(":app")
FILEEOF

# ═══════════════════════════════════════════════
# gradle-wrapper.properties
# ═══════════════════════════════════════════════
cat > gradle/wrapper/gradle-wrapper.properties << 'FILEEOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
FILEEOF

# ═══════════════════════════════════════════════
# app/build.gradle.kts
# ═══════════════════════════════════════════════
cat > app/build.gradle.kts << 'FILEEOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.apppair"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.apppair"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")

    implementation("com.google.android.material:material:1.11.0")

    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

kapt {
    correctErrorTypes = true
}
FILEEOF

# ═══════════════════════════════════════════════
# AndroidManifest.xml
# ═══════════════════════════════════════════════
cat > app/src/main/AndroidManifest.xml << 'FILEEOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission
        android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />

    <application
        android:name=".AppPairApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppPair">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.OverlayService"
            android:exported="false"
            android:foregroundServiceType="specialUse" />
    </application>
</manifest>
FILEEOF

# ═══════════════════════════════════════════════
# AppPairApplication.kt
# ═══════════════════════════════════════════════
cat > "$SRC/AppPairApplication.kt" << 'FILEEOF'
package com.apppair

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppPairApplication : Application()
FILEEOF

# ═══════════════════════════════════════════════
# data/model/AppModels.kt
# ═══════════════════════════════════════════════
cat > "$SRC/data/model/AppModels.kt" << 'FILEEOF'
package com.apppair.data.model

data class SelectedAppPair(
    val packageA: String?,
    val packageB: String?,
    val isServiceActive: Boolean = false
)

data class PermissionStatus(
    val overlayGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val batteryOptimized: Boolean = false
) {
    val allGranted: Boolean
        get() = overlayGranted && notificationsGranted
}
FILEEOF

# ═══════════════════════════════════════════════
# utils/PermissionUtils.kt
# ═══════════════════════════════════════════════
cat > "$SRC/utils/PermissionUtils.kt" << 'FILEEOF'
package com.apppair.utils

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.apppair.data.model.PermissionStatus

object PermissionUtils {

    fun checkPermissions(context: Context): PermissionStatus {
        val overlay = Settings.canDrawOverlays(context)

        val notifications = if (Build.VERSION.SDK_INT >= 33) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.areNotificationsEnabled
        } else {
            true
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOk = pm.isIgnoringBatteryOptimizations(context.packageName)

        return PermissionStatus(
            overlayGranted = overlay,
            notificationsGranted = notifications,
            batteryOptimized = batteryOk
        )
    }
}
FILEEOF

# ═══════════════════════════════════════════════
# data/preferences/AppPairPreferences.kt
# ═══════════════════════════════════════════════
cat > "$SRC/data/preferences/AppPairPreferences.kt" << 'FILEEOF'
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

    val serviceActive: Flow<Boolean> = context.dataStore.data.map {
        it[KEY_SERVICE_ACTIVE] ?: false
    }

    suspend fun saveSelectedApps(pkgA: String, pkgB: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP1] = pkgA
            prefs[KEY_APP2] = pkgB
        }
    }

    suspend fun setServiceActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVICE_ACTIVE] = active
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_APP1)
            prefs.remove(KEY_APP2)
            prefs.remove(KEY_SERVICE_ACTIVE)
        }
    }
}
FILEEOF

# ═══════════════════════════════════════════════
# data/repository/AppRepository.kt
# ═══════════════════════════════════════════════
cat > "$SRC/data/repository/AppRepository.kt" << 'FILEEOF'
package com.apppair.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.apppair.data.model.PermissionStatus
import com.apppair.data.model.SelectedAppPair
import com.apppair.data.preferences.AppPairPreferences
import com.apppair.utils.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?
)

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPairPreferences
) {
    val app1Package: Flow<String?> = preferences.app1Package
    val app2Package: Flow<String?> = preferences.app2Package
    val serviceActive: Flow<Boolean> = preferences.serviceActive

    val hasSelection: Flow<Boolean> = combine(
        preferences.app1Package,
        preferences.app2Package
    ) { app1, app2 ->
        app1 != null && app2 != null
    }

    val selectedAppPairFlow: Flow<SelectedAppPair> = combine(
        preferences.app1Package,
        preferences.app2Package,
        preferences.serviceActive
    ) { app1, app2, svcActive ->
        SelectedAppPair(
            packageA = app1,
            packageB = app2,
            isServiceActive = svcActive
        )
    }

    suspend fun saveSelectedApps(pkgA: String, pkgB: String) {
        preferences.saveSelectedApps(pkgA, pkgB)
    }

    suspend fun selectApps(pkgA: String, pkgB: String) {
        preferences.saveSelectedApps(pkgA, pkgB)
    }

    suspend fun setServiceActive(active: Boolean) {
        preferences.setServiceActive(active)
    }

    suspend fun clearSelection() {
        preferences.clear()
    }

    fun checkPermissions(): PermissionStatus {
        return PermissionUtils.checkPermissions(context)
    }

    fun getInstalledApps(): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        return resolveInfos
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                InstalledApp(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
FILEEOF

# ═══════════════════════════════════════════════
# ui/MainViewModel.kt
# ═══════════════════════════════════════════════
cat > "$SRC/ui/MainViewModel.kt" << 'FILEEOF'
package com.apppair.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apppair.data.model.PermissionStatus
import com.apppair.data.repository.AppRepository
import com.apppair.data.repository.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppPairUiState(
    val isLoading: Boolean = true,
    val installedApps: List<InstalledApp> = emptyList(),
    val selectedApp1: InstalledApp? = null,
    val selectedApp2: InstalledApp? = null,
    val isServiceRunning: Boolean = false,
    val permissions: PermissionStatus = PermissionStatus(),
    val error: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPairUiState())
    val uiState: StateFlow<AppPairUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        observeServiceState()
        refreshPermissions()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val apps = repository.getInstalledApps()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    installedApps = apps,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load apps: ${e.message}"
                )
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            repository.serviceActive.collect { active ->
                _uiState.value = _uiState.value.copy(isServiceRunning = active)
            }
        }
    }

    fun refreshPermissions() {
        val status = repository.checkPermissions()
        _uiState.value = _uiState.value.copy(permissions = status)
    }

    fun selectApp1(app: InstalledApp) {
        _uiState.value = _uiState.value.copy(selectedApp1 = app)
        saveIfComplete()
    }

    fun selectApp2(app: InstalledApp) {
        _uiState.value = _uiState.value.copy(selectedApp2 = app)
        saveIfComplete()
    }

    private fun saveIfComplete() {
        val s = _uiState.value
        if (s.selectedApp1 != null && s.selectedApp2 != null) {
            viewModelScope.launch {
                repository.saveSelectedApps(
                    s.selectedApp1!!.packageName,
                    s.selectedApp2!!.packageName
                )
            }
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedApp1 = null,
            selectedApp2 = null
        )
        viewModelScope.launch {
            repository.clearSelection()
        }
    }

    fun setServiceRunning(active: Boolean) {
        viewModelScope.launch {
            repository.setServiceActive(active)
        }
    }
}
FILEEOF

# ═══════════════════════════════════════════════
# ui/MainActivity.kt
# ═══════════════════════════════════════════════
cat > "$SRC/ui/MainActivity.kt" << 'FILEEOF'
package com.apppair.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.apppair.ui.theme.AppPairTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppPairTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppSelectionScreen()
                }
            }
        }
    }
}
FILEEOF

# ═══════════════════════════════════════════════
# ui/AppSelectionScreen.kt
# ═══════════════════════════════════════════════
cat > "$SRC/ui/AppSelectionScreen.kt" << 'FILEEOF'
package com.apppair.ui

import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Material
