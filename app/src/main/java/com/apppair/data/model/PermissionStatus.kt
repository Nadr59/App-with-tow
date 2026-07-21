package com.apppair.data.model

data class PermissionStatus(
    val hasOverlayPermission: Boolean = false,
    val hasBatteryOptimizationIgnored: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasQueryAllPackagesPermission: Boolean = true
) {
    val areAllRequiredPermissionsGranted: Boolean
        get() = hasOverlayPermission && hasBatteryOptimizationIgnored && hasNotificationPermission
}
