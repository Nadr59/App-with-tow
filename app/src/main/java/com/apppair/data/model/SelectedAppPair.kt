package com.apppair.data.model

data class SelectedAppPair(
    val packageA: String? = null,
    val packageB: String? = null,
    val appInfoA: AppInfo? = null,
    val appInfoB: AppInfo? = null,
    val isServiceActive: Boolean = false,
    val widgetX: Int = 100,
    val widgetY: Int = 300,
    val isMinimized: Boolean = false
) {
    val isReady: Boolean
        get() = !packageA.isNullOrBlank() && !packageB.isNullOrBlank() && packageA != packageB
}
