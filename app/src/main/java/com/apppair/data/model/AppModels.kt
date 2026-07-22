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
