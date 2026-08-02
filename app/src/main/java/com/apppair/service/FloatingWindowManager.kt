package com.nadrlab.apppair.service

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.WindowManager

class FloatingWindowManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingWindowMgr"
        private const val MINI_W = 220
        private const val MINI_H = 340
    }

    private val screenBounds = Rect()
    private var floatingBounds = Rect()
    private var isMinimized = false
    var app1Package: String = ""
        private set
    var isActive: Boolean = false
        private set

    init {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealSize(size)
        screenBounds.set(0, 0, size.x, size.y)
        resetBounds()
    }

    private fun resetBounds() {
        val w = (screenBounds.width() * 0.6f).toInt()
        val h = (screenBounds.height() * 0.7f).toInt()
        val x = (screenBounds.width() - w) / 2
        val y = 150
        floatingBounds.set(x, y, x + w, y + h)
    }

    fun isFreeformSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        return try {
            context.packageManager.hasSystemFeature("android.software.freeform_window_management") ||
                true // نجرب دائماً
        } catch (e: Exception) {
            false
        }
    }

    // ═══ تشغيل App 1 في نافذة منبثقة ═══
    fun launchInFloatingWindow(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "Freeform requires Android N+")
            return launchNormal(packageName)
        }

        app1Package = packageName
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: run {
            Log.e(TAG, "No launch intent for $packageName")
            return false
        }

        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
        )

        val bounds = if (isMinimized) miniBounds() else Rect(floatingBounds)

        val options = ActivityOptions.makeBasic()
        options.launchBounds = bounds

        return try {
            context.startActivity(intent, options.toBundle())
            isActive = true
            Log.d(TAG, "Launched $packageName in freeform: $bounds")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Freeform failed, trying normal", e)
            launchNormal(packageName)
        }
    }

    private fun launchNormal(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            isActive = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Normal launch failed", e)
            false
        }
    }

    // ═══ تصغير النافذة ═══
    fun minimize() {
        if (!isActive || app1Package.isBlank()) return
        isMinimized = true
        launchInFloatingWindow(app1Package)
    }

    // ═══ تكبير النافذة ═══
    fun maximize() {
        if (!isActive || app1Package.isBlank()) return
        isMinimized = false
        floatingBounds.set(screenBounds)
        launchInFloatingWindow(app1Package)
    }

    // ═══ استعادة الحجم الأصلي ═══
    fun restore() {
        if (!isActive || app1Package.isBlank()) return
        isMinimized = false
        resetBounds()
        launchInFloatingWindow(app1Package)
    }

    // ═══ تحريك ═══
    fun moveBy(dx: Int, dy: Int) {
        floatingBounds.offset(dx, dy)
        // حدود الشاشة
        if (floatingBounds.left < 0) floatingBounds.offset(-floatingBounds.left, 0)
        if (floatingBounds.top < 0) floatingBounds.offset(0, -floatingBounds.top)
        if (floatingBounds.right > screenBounds.width()) {
            floatingBounds.offset(screenBounds.width() - floatingBounds.right, 0)
        }
        if (floatingBounds.bottom > screenBounds.height()) {
            floatingBounds.offset(0, screenBounds.height() - floatingBounds.bottom)
        }
    }

    // ═══ تغيير الحجم ═══
    fun resize(width: Int, height: Int) {
        val w = width.coerceIn(300, screenBounds.width())
        val h = height.coerceIn(400, screenBounds.height())
        floatingBounds.right = floatingBounds.left + w
        floatingBounds.bottom = floatingBounds.top + h
    }

    // ═══ حجم مصغر ═══
    private fun miniBounds(): Rect {
        val x = screenBounds.right - MINI_W - 20
        val y = 200
        return Rect(x, y, x + MINI_W, y + MINI_H)
    }

    // ═══ إعادة تشغيل App 1 إذا مات ═══
    fun relaunchIfNeeded(): Boolean {
        if (!isActive || app1Package.isBlank()) return false
        if (!isAppRunning(app1Package)) {
            Log.d(TAG, "App1 died, relaunching...")
            return launchInFloatingWindow(app1Package)
        }
        return true
    }

    private fun isAppRunning(packageName: String): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return try {
            @Suppress("DEPRECATION")
            am.runningAppProcesses?.any {
                it.processName == packageName &&
                        it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        isActive = false
        isMinimized = false
        app1Package = ""
        resetBounds()
    }

    fun getState(): String {
        return "active=$isActive minimized=$isMinimized bounds=$floatingBounds pkg=$app1Package"
    }
}
