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

    private var windowManager: WindowManager? = null
    private var overlayView: LinearLayout? = null
    private var app1Package: String? = null
    private var app2Package: String? = null

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

    private fun switchToApp(packageName: String?) {
        packageName ?: return
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        startActivity(intent)
    }

    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
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

        // ═══════════════════════════════════════════
        // سحب الزر إلى أي موقع في الشاشة
        // ═══════════════════════════════════════════
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!isDragging && (dx * dx + dy * dy) > 400) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        windowManager?.updateViewLayout(overlayView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(overlayView, params)
    }

    private fun removeOverlay() {
        overlayView?.let { windowManager?.removeView(it) }
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
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
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
