package com.apppair.service

import android.app.ActivityManager
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

    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null
    private var app1Package: String? = null
    private var app2Package: String? = null

    // ═══════════════════════════════════════════
    // تتبع معرّفات المهام (Task IDs)
    // ═══════════════════════════════════════════
    private var app1TaskId: Int = -1
    private var app2TaskId: Int = -1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        app1Package = intent?.getStringExtra(EXTRA_APP1)
        app2Package = intent?.getStringExtra(EXTRA_APP2)

        if (overlayView == null && app1Package != null && app2Package != null) {
            showOverlay()
        }

        return START_STICKY
    }

    // ═══════════════════════════════════════════
    // التبديل بين التطبيقات (الحل الجوهري)
    // ═══════════════════════════════════════════
    private fun switchToApp(packageName: String?) {
        packageName ?: return

        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            // ── الخطوة 1: ابحث عن المهمة الحالية للتطبيق ──
            val taskId = findTaskId(am, packageName)

            if (taskId != -1) {
                // ── التطبيق يعمل بالفعل → أعد للأمام بدون إعادة تحميل ──
                am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
                saveTaskId(packageName, taskId)
            } else {
                // ── التطبيق غير موجود في المهام → افتحه لأول مرة ──
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    startActivity(launchIntent)

                    // انتظر قليلاً ثم خزّن معرّف المهمة
                    android.os.Handler(mainLooper).postDelayed({
                        val newTaskId = findTaskId(am, packageName)
                        saveTaskId(packageName, newTaskId)
                    }, 500)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // ── خطة بديلة: فتح عادي ──
            try {
                val fallback = packageManager.getLaunchIntentForPackage(packageName)
                if (fallback != null) {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(fallback)
                }
            } catch (_: Exception) {}
        }
    }

    // ═══════════════════════════════════════════
    // البحث عن معرّف المهمة للتطبيق
    // ═══════════════════════════════════════════
    private fun findTaskId(am: ActivityManager, packageName: String): Int {
        // ── الطريقة 1: من getRunningTasks ──
        try {
            @Suppress("DEPRECATION")
            val tasks = am.getRunningTasks(50)
            for (task in tasks) {
                if (task.baseActivity?.packageName == packageName ||
                    task.topActivity?.packageName == packageName
                ) {
                    return task.taskId
                }
            }
        } catch (_: Exception) {}

        // ── الطريقة 2: من appTasks (Android 5.0+) ──
        try {
            val appTasks = am.appTasks
            for (appTask in appTasks) {
                val taskInfo = appTask.taskInfo
                if (taskInfo.baseActivity?.packageName == packageName ||
                    taskInfo.topActivity?.packageName == packageName
                ) {
                    return taskInfo.taskId
                }
            }
        } catch (_: Exception) {}

        return -1
    }

    // ═══════════════════════════════════════════
    // حفظ معرّف المهمة للسرعة
    // ═══════════════════════════════════════════
    private fun saveTaskId(packageName: String, taskId: Int) {
        if (taskId == -1) return
        when (packageName) {
            app1Package -> app1TaskId = taskId
            app2Package -> app2TaskId = taskId
        }
    }

    // ═══════════════════════════════════════════
    // إنشاء الزر العائم
    // ═══════════════════════════════════════════
    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }

        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xE61A1A1A.toInt())
            setPadding(32, 20, 32, 20)
        }

        val btn1 = TextView(this).apply {
            text = "  App 1  "
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setPadding(8, 8, 8, 8)
            setOnClickListener { switchToApp(app1Package) }
        }

        val divider = TextView(this).apply {
            text = " │ "
            setTextColor(0xFF666666.toInt())
            textSize = 14f
        }

        val btn2 = TextView(this).apply {
            text = "  App 2  "
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            setPadding(8, 8, 8, 8)
            setOnClickListener { switchToApp(app2Package) }
        }

        overlayView?.addView(btn1)
        overlayView?.addView(divider)
        overlayView?.addView(btn2)

        // ── جعل الزر قابل للسحب ──
        var initX = 0
        var initY = 0
        var initTouchX = 0f
        var initTouchY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x
                    initY = params.y
                    initTouchX = event.rawX
                    initTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initX - (event.rawX - initTouchX).toInt()
                    params.y = initY + (event.rawY - initTouchY).toInt()
                    try {
                        windowManager?.updateViewLayout(overlayView, params)
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_channel_name))
            .setContentText("Tap to manage")
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
