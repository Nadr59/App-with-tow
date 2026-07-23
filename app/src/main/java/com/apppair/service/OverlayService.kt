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
import android.os.Handler
import android.os.Looper
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

    // ═══ تتبع المهام ═══
    private var lastLaunchedPackage: String? = null
    private val handler = Handler(Looper.getMainLooper())

    // ═══ مراقبة حية ═══
    private val watchdog = object : Runnable {
        override fun run() {
            try {
                checkAppsAlive()
            } catch (_: Exception) {}
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            removeOverlay()
            handler.removeCallbacks(watchdog)
            stopSelf()
            return START_NOT_STICKY
        }

        app1Package = intent?.getStringExtra(EXTRA_APP1)
        app2Package = intent?.getStringExtra(EXTRA_APP2)

        if (overlayView == null && app1Package != null && app2Package != null) {
            showOverlay()
            handler.post(watchdog)
        }

        return START_STICKY
    }

    // ═══════════════════════════════════════════════════════
    // التبديل الذكي: يكتشف إذا التطبيق قُتل ويُعيد تشغيله
    // ═══════════════════════════════════════════════════════
    private fun switchToApp(packageName: String?) {
        packageName ?: return

        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val isRunning = isAppInForeground(am, packageName)

            if (isRunning) {
                // ═══ التطبيق يعمل: أعد للأمام بدون إعادة تحميل ═══
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    startActivity(launchIntent)
                }
            } else {
                // ═══ التطبيق قُتل: افتحه مع حماية من الإغلاق ═══
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    startActivity(launchIntent)
                }
            }

            lastLaunchedPackage = packageName

        } catch (e: Exception) {
            e.printStackTrace()
            // ═══ خطة بديلة ═══
            try {
                val fallback = packageManager.getLaunchIntentForPackage(packageName)
                fallback?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (fallback != null) startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    // ═══════════════════════════════════════════════════════
    // فحص: هل التطبيق يعمل في المهام؟
    // ═══════════════════════════════════════════════════════
    private fun isAppInForeground(am: ActivityManager, packageName: String): Boolean {
        try {
            // الطريقة 1: من الأنشطة الجارية
            val tasks = am.appTasks
            for (task in tasks) {
                val info = task.taskInfo
                if (info.topActivity?.packageName == packageName ||
                    info.baseActivity?.packageName == packageName
                ) {
                    return true
                }
            }
        } catch (_: Exception) {}

        try {
            // الطريقة 2: من العمليات الجارية
            @Suppress("DEPRECATION")
            val processes = am.runningAppProcesses
            if (processes != null) {
                for (proc in processes) {
                    if (proc.processName == packageName ||
                        proc.processName.startsWith("$packageName:")
                    ) {
                        if (proc.importance <=
                            android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
                        ) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return false
    }

    // ═══════════════════════════════════════════════════════
    // مراقب: يفحص التطبيقات المختارة كل 5 ثوانٍ
    // ═══════════════════════════════════════════════════════
    private fun checkAppsAlive() {
        val pkg1 = app1Package ?: return
        val pkg2 = app2Package ?: return

        // تحقق من أن كلا التطبيقين مثبتان
        val app1Installed = isAppInstalled(pkg1)
        val app2Installed = isAppInstalled(pkg2)

        if (!app1Installed || !app2Installed) {
            // أحد التطبيقات حُذف — أرسل إشعار
            sendAlertNotification("One of your selected apps was uninstalled!")
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun sendAlertNotification(message: String) {
        try {
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("AppPair Alert")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .build()
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(2002, notif)
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    // واجهة المستخدم
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
        handler.removeCallbacks(watchdog)
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
