package com.apppair.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.apppair.R
import com.apppair.ui.MainActivity

class OverlayService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "app_pair_overlay"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.apppair.STOP"
        const val EXTRA_APP1 = "app1_package"
        const val EXTRA_APP2 = "app2_package"
        private const val TAG = "OverlayService"
        private const val CHECK_INTERVAL = 15_000L
    }

    private var windowManager: WindowManager? = null
    private var widgetView: View? = null
    private var isExpanded = false

    private var app1Package: String = ""
    private var app2Package: String = ""
    private var isRunning = false

    private var screenW = 0
    private var screenH = 0
    private var app1Bounds = Rect()

    private val handler = Handler(Looper.getMainLooper())

    // ═══ فحص دوري: فقط تأكد أن App1 يعمل ═══
    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            if (isApp1Running()) {
                Log.d(TAG, "App1 is running, OK")
            } else {
                Log.d(TAG, "App1 died, relaunching in freeform")
                launchApp1Freeform()
            }

            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        screenW = metrics.widthPixels
        screenH = metrics.heightPixels
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopEverything()
            stopSelf()
            return START_NOT_STICKY
        }

        val newApp1 = intent?.getStringExtra(EXTRA_APP1) ?: ""
        val newApp2 = intent?.getStringExtra(EXTRA_APP2) ?: ""

        if (newApp1.isNotBlank() && newApp2.isNotBlank()) {
            app1Package = newApp1
            app2Package = newApp2

            if (!isRunning) {
                isRunning = true
                calculateBounds()
                showWidget()

                // افتح App1 في نافذة مصغرة
                launchApp1Freeform()

                // انتظر ثم افتح App2
                handler.postDelayed({ launchApp2() }, 1500)

                // فحص دوري
                handler.postDelayed(checkRunnable, CHECK_INTERVAL)
            }
        }

        return START_STICKY
    }

    // ═══════════════════════════════════════════
    // حساب حجم النافذة المصغرة
    // ═══════════════════════════════════════════
    private fun calculateBounds() {
        val w = (screenW * 0.45).toInt()
        val h = (screenH * 0.50).toInt()
        val x = screenW - w - 20
        val y = 80
        app1Bounds.set(x, y, x + w, y + h)
    }

    // ═══════════════════════════════════════════
    // هل App1 يعمل حالياً؟
    // ═══════════════════════════════════════════
    private fun isApp1Running(): Boolean {
        if (app1Package.isBlank()) return false
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        try {
            for (task in am.appTasks) {
                val info = task.taskInfo
                if (info.baseActivity?.packageName == app1Package) {
                    return true
                }
            }
        } catch (_: Exception) {}

        // طريقة بديلة
        try {
            @Suppress("DEPRECATION")
            val processes = am.runningAppProcesses
            processes?.forEach {
                if (it.processName == app1Package &&
                    it.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
                ) return true
            }
        } catch (_: Exception) {}

        return false
    }

    // ═══════════════════════════════════════════
    // فتح App1 في نافذة مصغرة (Freeform)
    // ═══════════════════════════════════════════
    private fun launchApp1Freeform() {
        if (app1Package.isBlank()) return

        // ═══ أولاً: حاول إحضار النسخة الموجودة ═══
        if (bringExistingToFront()) {
            Log.d(TAG, "App1: brought existing to front")
            return
        }

        // ═══ ثانياً: افتح نسخة جديدة ═══
        val intent = packageManager.getLaunchIntentForPackage(app1Package) ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val options = ActivityOptions.makeBasic()
            options.setLaunchBounds(app1Bounds)

            // حاول تعيين وضع Freeform صراحةً
            try {
                ActivityOptions::class.java
                    .getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                    .invoke(options, 5) // WINDOWING_MODE_FREEFORM = 5
            } catch (_: Exception) {}

            try {
                startActivity(intent, options.toBundle())
                Log.d(TAG, "App1 launched FREEFORM: $app1Bounds")
                return
            } catch (e: Exception) {
                Log.e(TAG, "Freeform failed", e)
            }
        }

        // خطة بديلة: فتح عادي
        try {
            startActivity(intent)
            Log.d(TAG, "App1 launched NORMAL (fallback)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch App1", e)
        }
    }

    // ═══════════════════════════════════════════
    // إحضار النسخة الموجودة للأمام
    // ═══════════════════════════════════════════
    private fun bringExistingToFront(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        try {
            for (task in am.appTasks) {
                if (task.taskInfo.baseActivity?.packageName == app1Package) {
                    task.moveToFront()
                    Log.d(TAG, "App1: moved existing task to front")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "moveToFront failed", e)
        }
        return false
    }

    // ═══════════════════════════════════════════
    // فتح App2 عادي (ملء الشاشة)
    // ═══════════════════════════════════════════
    private fun launchApp2() {
        if (app2Package.isBlank()) return

        // حاول إحضار الموجود أولاً
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        try {
            for (task in am.appTasks) {
                if (task.taskInfo.baseActivity?.packageName == app2Package) {
                    task.moveToFront()
                    Log.d(TAG, "App2: moved existing to front")
                    return
                }
            }
        } catch (_: Exception) {}

        val intent = packageManager.getLaunchIntentForPackage(app2Package) ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
        try {
            startActivity(intent)
            Log.d(TAG, "App2 launched: $app2Package")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch App2", e)
        }
    }

    // ═══════════════════════════════════════════
    // الزر العائم
    // ═══════════════════════════════════════════
    @SuppressLint("ClickableViewAccessibility")
    private fun showWidget() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val size = (52 * density).toInt()

        widgetView = createWidgetView(density)

        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = (screenH * 0.4f).toInt()
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        widgetView?.setOnTouchListener { view, event ->
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
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                    }
                    if (isDragging && !isExpanded) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(view, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        if (isExpanded) collapseWidget(density)
                        else expandWidget(density)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(widgetView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show widget", e)
        }
    }

    private fun createWidgetView(density: Float): View {
        val size = (52 * density).toInt()

        val container = FrameLayout(this@OverlayService).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        val circle = View(this@OverlayService).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFE8C547.toInt())
                setStroke((2 * density).toInt(), 0xFF000000.toInt())
            }
            elevation = 8 * density
        }
        container.addView(circle)

        val icon = TextView(this@OverlayService).apply {
            text = "⇄"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(0xFF000000.toInt())
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
        container.addView(icon)

        val panel = createExpandedPanel(density)
        panel.visibility = View.GONE
        panel.tag = "expanded"
        container.addView(panel)

        return container
    }

    private fun createExpandedPanel(density: Float): LinearLayout {
        val panelW = (240 * density).toInt()
        val pad = (10 * density).toInt()

        return LinearLayout(this@OverlayService).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            layoutParams = FrameLayout.LayoutParams(
                panelW,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = (56 * density).toInt()
            }
            background = GradientDrawable().apply {
                cornerRadius = 12 * density
                setColor(0xDD1A1A1A.toInt())
                setStroke((1 * density).toInt(), 0xFFE8C547.toInt())
            }
            elevation = 10 * density

            addView(createAppButton(
                "▶ ${getAppName(app1Package)}", 0xFFE8C547.toInt(), density
            ) {
                launchApp1Freeform()
                collapseWidget(density)
            })

            addView(createSeparator(density))

            addView(createAppButton(
                "▶ ${getAppName(app2Package)}", 0xFF4CAF50.toInt(), density
            ) {
                launchApp2()
                collapseWidget(density)
            })

            addView(createSeparator(density))

            addView(createAppButton(
                "✕ إيقاف", 0xFFF44336.toInt(), density
            ) {
                stopEverything()
                stopSelf()
            })
        }
    }

    private fun createAppButton(
        text: String, color: Int, density: Float, onClick: () -> Unit
    ): TextView {
        val pad = (12 * density).toInt()
        return TextView(this@OverlayService).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, (10 * density).toInt(), pad, (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * density).toInt()
            )
            setOnClickListener { onClick() }
        }
    }

    private fun createSeparator(density: Float): View {
        return View(this@OverlayService).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt()
            ).apply {
                marginStart = (12 * density).toInt()
                marginEnd = (12 * density).toInt()
            }
        }
    }

    private fun expandWidget(density: Float) {
        isExpanded = true
        val container = widgetView as? FrameLayout ?: return
        val panel = container.findViewWithTag<View>("expanded") ?: return

        container.getChildAt(0).alpha = 0.3f
        container.getChildAt(1).alpha = 0.3f
        panel.visibility = View.VISIBLE
        panel.alpha = 0f
        panel.animate().alpha(1f).setDuration(200).start()

        val params = widgetView?.layoutParams as? WindowManager.LayoutParams ?: return
        params.width = (300 * density).toInt()
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        try {
            windowManager?.updateViewLayout(widgetView, params)
        } catch (_: Exception) {}
    }

    private fun collapseWidget(density: Float) {
        isExpanded = false
        val container = widgetView as? FrameLayout ?: return
        val panel = container.findViewWithTag<View>("expanded") ?: return

        panel.visibility = View.GONE
        container.getChildAt(0).alpha = 1f
        container.getChildAt(1).alpha = 1f

        val params = widgetView?.layoutParams as? WindowManager.LayoutParams ?: return
        val size = (52 * density).toInt()
        params.width = size
        params.height = size
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        try {
            windowManager?.updateViewLayout(widgetView, params)
        } catch (_: Exception) {}
    }

    private fun getAppName(pkg: String): String {
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast(".")
        }
    }

    private fun stopEverything() {
        isRunning = false
        handler.removeCallbacks(checkRunnable)
        removeWidget()
    }

    private fun removeWidget() {
        widgetView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        widgetView = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            .setContentText("${getAppName(app1Package)} ⇄ ${getAppName(app2Package)}")
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
