package com.apppair.ui.guide

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

data class OemGuideInfo(
    val manufacturerName: String,
    val steps: List<String>,
    val autoStartIntent: Intent? = null
)

object OemHelper {

    fun getDeviceGuideInfo(): OemGuideInfo {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                OemGuideInfo(
                    manufacturerName = "Xiaomi / Redmi / POCO (MIUI & HyperOS)",
                    steps = listOf(
                        "1. Enable Auto-Start: Tap 'Open Auto-Start Settings' below, find AppPair, and toggle Auto-Start to ON.",
                        "2. Set Battery Optimization to No Restrictions: Open App Details -> Battery Saver -> Choose 'No restrictions'.",
                        "3. Lock in Recent Apps: Open your device's recent task switcher, tap and hold (or swipe down on) the AppPair card, and tap the Lock icon so the system never clears it during memory cleanup."
                    ),
                    autoStartIntent = getXiaomiAutoStartIntent()
                )
            }
            manufacturer.contains("samsung") -> {
                OemGuideInfo(
                    manufacturerName = "Samsung (One UI)",
                    steps = listOf(
                        "1. Turn Off Unused App Sleeping: Open Settings -> Battery -> Background usage limits -> Turn off 'Put unused apps to sleep'.",
                        "2. Add to Never Sleeping Apps: In Background usage limits -> Tap 'Never sleeping apps' -> Add AppPair.",
                        "3. Disable Battery Optimization: Tap 'Open Battery Optimization' below and allow AppPair to run unrestricted."
                    ),
                    autoStartIntent = getSamsungAutoStartIntent()
                )
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                OemGuideInfo(
                    manufacturerName = "Huawei / Honor (EMUI & MagicUI)",
                    steps = listOf(
                        "1. Enable Manual App Launch: Open Settings -> Battery -> App Launch -> Find AppPair and turn OFF 'Manage automatically'.",
                        "2. Enable All Three Permissions: In the manual management popup, make sure 'Auto-launch', 'Secondary launch', and 'Run in background' are all checked ON.",
                        "3. Ignore Battery Optimization: Tap the button below to exempt AppPair from power saving checks."
                    ),
                    autoStartIntent = getHuaweiAutoStartIntent()
                )
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> {
                OemGuideInfo(
                    manufacturerName = "OPPO / Realme / OnePlus (ColorOS & OxygenOS)",
                    steps = listOf(
                        "1. Allow Auto-Launch: Open App Details -> Battery -> Allow background activity & Allow auto launch.",
                        "2. Lock in Recents: Open the recent apps screen, tap the 3 dots on the AppPair window, and select 'Lock'.",
                        "3. Turn off Battery Optimization: Ensure unrestricted power usage in system battery settings."
                    ),
                    autoStartIntent = getOppoAutoStartIntent()
                )
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                OemGuideInfo(
                    manufacturerName = "Vivo / iQOO (FuntouchOS & OriginOS)",
                    steps = listOf(
                        "1. Enable High Background Power Consumption: Open Settings -> Battery -> Background power consumption management -> Allow high background power consumption for AppPair.",
                        "2. Enable Auto-Start: Open system management/permissions -> Auto-start -> Enable AppPair.",
                        "3. Lock in Recents: Lock AppPair card inside the recent apps tray."
                    ),
                    autoStartIntent = getVivoAutoStartIntent()
                )
            }
            else -> {
                OemGuideInfo(
                    manufacturerName = "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} / Android Stock",
                    steps = listOf(
                        "1. Disable Battery Optimization: Tap 'Open Battery Optimization Settings' below and select 'Allow' or 'Unrestricted'.",
                        "2. Allow Background Activity: Open Application Details Settings -> Battery -> Allow background activity.",
                        "3. Pin/Lock in Recents: If your device supports pinning or locking apps in the recent tasks screen, lock AppPair to ensure uninterrupted dual-app switching."
                    ),
                    autoStartIntent = null
                )
            }
        }
    }

    private fun getXiaomiAutoStartIntent(): Intent {
        return Intent().apply {
            component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getSamsungAutoStartIntent(): Intent {
        return Intent().apply {
            component = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getHuaweiAutoStartIntent(): Intent {
        return Intent().apply {
            component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getOppoAutoStartIntent(): Intent {
        return Intent().apply {
            component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getVivoAutoStartIntent(): Intent {
        return Intent().apply {
            component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
