package com.apppair.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.*
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

    private var wm: WindowManager? = null
    private var ov: LinearLayout? = null
    private var p1: String? = null
    private var p2: String? = null

    override fun onCreate() { super.onCreate(); mkChan(); startForeground(NOTIFICATION_ID, mkNotif()) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) { rmOv(); stopSelf(); return START_NOT_STICKY }
        p1 = intent?.getStringExtra(EXTRA_APP1); p2 = intent?.getStringExtra(EXTRA_APP2)
        if (ov == null && p1 != null && p2 != null) show()
        return START_STICKY
    }

    private fun show() {
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val pr = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x = 16; y = 200 }
        ov = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xE61A1A1A.toInt()); setPadding(32, 20, 32, 20) }
        ov?.addView(TextView(this).apply { text = "App 1"; setTextColor(-1); textSize = 15f; setOnClickListener { go(p1) } })
        ov?.addView(TextView(this).apply { text = "  |  "; setTextColor(0xFF888888.toInt()); textSize = 15f })
        ov?.addView(TextView(this).apply { text = "App 2"; setTextColor(-1); textSize = 15f; setOnClickListener { go(p2) } })
        var ix = 0; var iy = 0; var tx = 0f; var ty = 0f
        ov?.setOnTouchListener { _, e -> when (e.action) { MotionEvent.ACTION_DOWN -> { ix = pr.x; iy = pr.y; tx = e.rawX; ty = e.rawY; true }; MotionEvent.ACTION_MOVE -> { pr.x = ix - (e.rawX - tx).toInt(); pr.y = iy + (e.rawY - ty).toInt(); try { wm?.updateViewLayout(ov, pr) } catch (_: Exception) {}; true }; else -> false } }
        try { wm?.addView(ov, pr) } catch (_: Exception) {}
    }

    private fun go(pkg: String?) { pkg ?: return; try { val i = packageManager.getLaunchIntentForPackage(pkg); if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); startActivity(i) } } catch (_: Exception) {} }
    private fun rmOv() { ov?.let { try { wm?.removeView(it) } catch (_: Exception) {} }; ov = null }
    private fun mkChan() { val c = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW); c.description = getString(R.string.notification_channel_desc); (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(c) }
    private fun mkNotif(): Notification { val o = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE); val s = PendingIntent.getService(this, 0, Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE); return NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle(getString(R.string.notification_channel_name)).setContentText("Tap to manage").setSmallIcon(android.R.drawable.ic_menu_swap).setContentIntent(o).addAction(android.R.drawable.ic_delete, "Stop", s).setOngoing(true).build() }
    override fun onDestroy() { rmOv(); super.onDestroy() }
    override fun onBind(intent: Intent): IBinder? { super.onBind(intent); return null }
}
