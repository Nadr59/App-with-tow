package com.apppair.service

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager

class FloatingWindowManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingWindowMgr"
    }

    private var screenW = 0
    private var screenH = 0
    private var windowW = 0
    private var windowH = 0
    private var windowX = 0
    private var windowY = 0
    private var isMinimized = false
    var app1Package: String = ""
        private set
    var isActive: Boolean = false
        private set

    init {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        screenW = metrics.widthPixels
        screenH = metrics.heightPixels
        resetBounds()
    }

    private fun resetBounds() {
        windowW = (screenW * 0.70).toInt()
        windowH = (screenH * 0.60).toInt()
        windowX = (screenW - windowW) / 2
        windowY = (screenH - windowH) / 3
    }

    fun launchInFloatingWindow(packageName: String): Boolean {
        app1Package = packageName
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false

        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )

        val bounds = if (isMinimized) {
            val miniW = 220
            val miniH = 340
            val x = screenW - miniW - 20
            Rect(x, 200, x + miniW, 200 + miniH)
        } else {
            Rect(windowX, windowY, windowX + windowW, windowY + windowH)
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val options = ActivityOptions.makeBasic()
                options.setLaunchBounds(bounds)
                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
            }
            isActive = true
            Log.d(TAG, "Launched $packageName in freeform: $bounds")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Freeform failed", e)
            try {
                context.startActivity(intent)
                isActive = true
                true
            } catch (e2: Exception) {
                Log.e(TAG, "Normal launch failed", e2)
                false
            }
        }
    }

    fun minimize() {
        isMinimized = true
        if (app1Package.isNotBlank()) {
            launchInFloatingWindow(app1Package)
        }
    }

    fun maximize() {
        isMinimized = false
        windowX = 0
        windowY = 0
        windowW = screenW
        windowH = screenH
        if (app1Package.isNotBlank()) {
            launchInFloatingWindow(app1Package)
        }
    }

    fun restore() {
        isMinimized = false
        resetBounds()
        if (app1Package.isNotBlank()) {
            launchInFloatingWindow(app1Package)
        }
    }

    fun moveBy(dx: Int, dy: Int) {
        windowX += dx
        windowY += dy
        windowX = windowX.coerceIn(0, screenW - windowW)
        windowY = windowY.coerceIn(0, screenH - windowH)
    }

    fun close() {
        isActive = false
        isMinimized = false
        app1Package = ""
        resetBounds()
    }
}
