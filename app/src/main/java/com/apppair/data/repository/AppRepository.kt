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
        preferences.app1Package, preferences.app2Package
    ) { a, b -> a != null && b != null }

    val selectedAppPairFlow: Flow<SelectedAppPair> = combine(
        preferences.app1Package, preferences.app2Package, preferences.serviceActive
    ) { a, b, s -> SelectedAppPair(a, b, s) }

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
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        }
        return false
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
