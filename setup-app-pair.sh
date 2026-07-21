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
# ═══ ui/AppSelectionScreen.kt ═══
cat > "$SRC/ui/AppSelectionScreen.kt" << 'EOF'
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.apppair.data.repository.InstalledApp
import com.apppair.service.OverlayService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectingSlot by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Pair Switcher") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            if (!uiState.permissions.overlayGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Overlay permission required", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Needed for the floating switcher", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}"))
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Grant") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text("Select two apps:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSlotCard("App 1", uiState.selectedApp1, selectingSlot == 1, { selectingSlot = 1 }, Modifier.weight(1f))
                AppSlotCard("App 2", uiState.selectedApp2, selectingSlot == 2, { selectingSlot = 2 }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))

            if (uiState.selectedApp1 != null && uiState.selectedApp2 != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val i = Intent(context, OverlayService::class.java).apply {
                                putExtra(OverlayService.EXTRA_APP1, uiState.selectedApp1!!.packageName)
                                putExtra(OverlayService.EXTRA_APP2, uiState.selectedApp2!!.packageName)
                            }
                            context.startForegroundService(i)
                            viewModel.setServiceRunning(true)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isServiceRunning
                    ) { Text("Start") }

                    OutlinedButton(
                        onClick = {
                            val i = Intent(context, OverlayService::class.java).apply { action = OverlayService.ACTION_STOP }
                            context.startService(i)
                            viewModel.setServiceRunning(false)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isServiceRunning
                    ) { Text("Stop") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear Selection")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectingSlot > 0) {
                Text("Pick app for Slot $selectingSlot:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }
                selectingSlot > 0 -> {
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(uiState.installedApps) { app ->
                            AppListItem(app) {
                                when (selectingSlot) { 1 -> viewModel.selectApp1(app); 2 -> viewModel.selectApp2(app) }
                                selectingSlot = 0
                            }
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tap App 1 or App 2 above", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun AppSlotCard(label: String, app: InstalledApp?, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (app != null) {
                app.icon?.let { d ->
                    Image(bitmap = d.toBitmap(48, 48, Bitmap.Config.ARGB_8888).asImageBitmap(), contentDescription = app.name, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(app.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Tap to select", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun AppListItem(app: InstalledApp, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            app.icon?.let { d ->
                Image(bitmap = d.toBitmap(40, 40, Bitmap.Config.ARGB_8888).asImageBitmap(), contentDescription = app.name, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(app.name, style = MaterialTheme.typography.bodyLarge)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
EOF

echo ">>> Part 4a done (AppSelectionScreen)"
# ═══ service/OverlayService.kt ═══
cat > "$SRC/service/OverlayService.kt" << 'EOF'
package com.apppair.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.apppair.R
import com.apppair.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OverlayService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "app_pair_overlay"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.apppair.STOP"
        const val EXTRA_APP1 = "app1_package"
        const val EXTRA_APP2 = "app2_package"
    }

    private var wm: WindowManager? = null
    private var overlay: LinearLayout? = null
    private var pkg1: String? = null
    private var pkg2: String? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotif())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) { removeOverlay(); stopSelf(); return START_NOT_STICKY }
        pkg1 = intent?.getStringExtra(EXTRA_APP1)
        pkg2 = intent?.getStringExtra(EXTRA_APP2)
        if (overlay == null && pkg1 != null && pkg2 != null) showOverlay()
        return START_STICKY
    }

    private fun showOverlay() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 16; y = 200 }

        overlay = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xE61A1A1A.toInt())
            setPadding(32, 20, 32, 20)
        }

        val b1 = TextView(this).apply { text = "App 1"; setTextColor(-1); textSize = 15f; setOnClickListener { go(pkg1) } }
        val dv = TextView(this).apply { text = "  |  "; setTextColor(0xFF888888.toInt()); textSize = 15f }
        val b2 = TextView(this).apply { text = "App 2"; setTextColor(-1); textSize = 15f; setOnClickListener { go(pkg2) } }

        overlay?.addView(b1); overlay?.addView(dv); overlay?.addView(b2)

        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f
        overlay?.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { ix = p.x; iy = p.y; tx = e.rawX; ty = e.rawY; true }
                MotionEvent.ACTION_MOVE -> { p.x = ix - (e.rawX - tx).toInt(); p.y = iy + (e.rawY - ty).toInt(); try { wm?.updateViewLayout(overlay, p) } catch (_: Exception) {}; true }
                else -> false
            }
        }

        try { wm?.addView(overlay, p) } catch (e: Exception) { e.printStackTrace() }
    }

    private fun go(pkg: String?) {
        pkg ?: return
        try {
            val i = packageManager.getLaunchIntentForPackage(pkg)
            if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); startActivity(i) }
        } catch (_: Exception) {}
    }

    private fun removeOverlay() { overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }; overlay = null }

    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
        ch.description = getString(R.string.notification_channel_desc)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 0, Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_channel_name))
            .setContentText("Tap to manage")
            .setSmallIcon(android.R.drawable.ic_menu_swap)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_delete, "Stop", stop)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() { removeOverlay(); super.onDestroy() }
    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }
}
EOF

echo ">>> Part 4b done (OverlayService)"
# ═══ Resources ═══

cat > "$RES/values/strings.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AppPair</string>
    <string name="notification_channel_name">App Pair Service</string>
    <string name="notification_channel_desc">Keeps the app pair switcher running</string>
    <string name="perm_overlay_title">Overlay permission</string>
    <string name="perm_overlay_desc">Required for floating switcher</string>
    <string name="perm_battery_title">Battery optimization</string>
    <string name="perm_battery_desc">Allow background operation</string>
    <string name="perm_notification_title">Notifications</string>
    <string name="perm_notification_desc">Required for service notifications</string>
</resources>
EOF

cat > "$RES/values/themes.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.AppPair" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
EOF

cat > "$RES/values-night/themes.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.AppPair" parent="Theme.Material3.Dark.NoActionBar">
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
    </style>
</resources>
EOF

cat > "$RES/values/colors.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#0D0D0D</color>
</resources>
EOF

cat > "$RES/drawable/ic_launcher_foreground.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#1B5E20" android:pathData="M54,54m-40,0a40,40 0,1 1,80 0a40,40 0,1 1,-80 0"/>
    <path android:fillColor="#FFFFFF" android:pathData="M38,46 L50,46 L50,40 L62,54 L50,68 L50,62 L38,62 Z"/>
    <path android:fillColor="#FFFFFF" android:pathData="M70,46 L58,46 L58,40 L46,54 L58,68 L58,62 L70,62 Z"/>
</vector>
EOF

cat > "$RES/drawable/ic_launcher_background.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0D0D0D" android:pathData="M0,0h108v108h-108z"/>
</vector>
EOF

cat > "$RES/mipmap-anydpi-v26/ic_launcher.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
EOF

cat > "$RES/mipmap-anydpi-v26/ic_launcher_round.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
EOF

echo ">>> Part 4c done (Resources)"
# ═══ GitHub Actions Workflow ═══

cat > .github/workflows/android-build.yml << 'EOF'
name: Android CI - Build APKs

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-debug:
    name: Build Debug APK
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        id: setup-java
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.5'

      - name: Install Android SDK
        env:
          JAVA_HOME: ${{ steps.setup-java.outputs.java-home }}
          ANDROID_SDK_ROOT: ${{ runner.temp }}/android-sdk
        run: |
          mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
          wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O /tmp/cmd.zip
          unzip -q /tmp/cmd.zip -d /tmp/cmd-tmp
          mv /tmp/cmd-tmp/cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest"
          rm -rf /tmp/cmd.zip /tmp/cmd-tmp
          yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" --licenses > /dev/null 2>&1 || true
          "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
          echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT" >> "$GITHUB_ENV"

      - name: Create local.properties
        run: echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

      - name: Build Debug APK
        run: |
          if [ -x "./gradlew" ]; then
            ./gradlew assembleDebug --no-daemon --stacktrace
          else
            gradle assembleDebug --no-daemon --stacktrace
          fi

      - uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: warn

  build-release:
    name: Build Release APK
    runs-on: ubuntu-latest
    needs: build-debug
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        id: setup-java
        with:
          distribution: temurin
          java-version: '17'

      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.5'

      - name: Install Android SDK
        env:
          JAVA_HOME: ${{ steps.setup-java.outputs.java-home }}
          ANDROID_SDK_ROOT: ${{ runner.temp }}/android-sdk
        run: |
          mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
          wget -q "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -O /tmp/cmd.zip
          unzip -q /tmp/cmd.zip -d /tmp/cmd-tmp
          mv /tmp/cmd-tmp/cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest"
          rm -rf /tmp/cmd.zip /tmp/cmd-tmp
          yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" --licenses > /dev/null 2>&1 || true
          "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
          echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT" >> "$GITHUB_ENV"

      - name: Create local.properties
        run: echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

      - name: Check for keystore
        id: check-keystore
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          if [ -n "$KEYSTORE_BASE64" ]; then
            echo "has_keystore=true" >> "$GITHUB_OUTPUT"
          else
            echo "has_keystore=false" >> "$GITHUB_OUTPUT"
          fi

      - name: Decode keystore
        if: steps.check-keystore.outputs.has_keystore == 'true'
        env:
          KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}
        run: |
          mkdir -p keystore
          echo "$KEYSTORE_BASE64" | base64 --decode > keystore/release.jks

      - name: Build Signed Release
        if: steps.check-keystore.outputs.has_keystore == 'true'
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: |
          gradle assembleRelease --no-daemon --stacktrace \
            -Pandroid.injected.signing.store.file="$GITHUB_WORKSPACE/keystore/release.jks" \
            -Pandroid.injected.signing.store.password="$KEYSTORE_PASSWORD" \
            -Pandroid.injected.signing.key.alias="$KEY_ALIAS" \
            -Pandroid.injected.signing.key.password="$KEY_PASSWORD"

      - name: Build Unsigned Release
        if: steps.check-keystore.outputs.has_keystore != 'true'
        run: gradle assembleRelease --no-daemon --stacktrace

      - uses: actions/upload-artifact@v4
        with:
          name: app-release
          path: app/build/outputs/apk/release/app-release*.apk
          if-no-files-found: warn
EOF

echo ""
echo "============================================"
echo "  ALL FILES CREATED SUCCESSFULLY!"
echo "============================================"
echo ""
echo "Next steps:"
echo "  git add ."
echo "  git commit -m 'Fresh project setup'"
echo "  git push --force"
