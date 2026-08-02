package com.apppair.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
        private const val RELAUNCH_INTERVAL = 10_000L
    }

    private var windowManager: WindowManager? = null
    private var widgetView: View? = null
    private var app1Package: String = ""
    private var app2Package: String = ""
    private var isRunning = false
    private var isExpanded = false

    private lateinit var floatingMgr: FloatingWindowManager
    private val handler = Handler(Looper.getMainLooper())

    // ═══ فحص دوري: إعادة تشغيل App1 إذا مات ═══
    private val relaunchRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (floatingMgr.isActive && floatingMgr.app1Package.isNotBlank()) {
                // حاول إعادة تشغيل App1
                floatingMgr.launchInFloatingWindow(floatingMgr.app1Package)
            }
            handler.postDelayed(this, RELAUNCH_INTERVAL)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        floatingMgr = FloatingWindowManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopEverything()
            stopSelf()
            return START_NOT_STICKY
        }

        app1Package = intent?.getStringExtra(EXTRA_APP1) ?: ""
        app2Package = intent?.getStringExtra(EXTRA_APP2) ?: ""

        if (app1Package.isNotBlank() && app2Package.isNotBlank() && !isRunning) {
            isRunning = true
            showWidget()
            launchApp1()
            handler.postDelayed(relaunchRunnable, RELAUNCH_INTERVAL)
        }

        return START_STICKY
    }

    // ═══════════════════════════════════════════
    // تشغيل App1 في نافذة منبثقة
    // ═══════════════════════════════════════════
    private fun launchApp1() {
        floatingMgr.launchInFloatingWindow(app1Package)
    }

    // ═══════════════════════════════════════════
    // تشغيل App2 عادي (ملء الشاشة)
    // ═══════════════════════════════════════════
    private fun switchToApp2() {
        val intent = packageManager.getLaunchIntentForPackage(app2Package) ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch App2", e)
        }
    }

    // ═══════════════════════════════════════════
    // الزر العائم الدائري
    // ═══════════════════════════════════════════
    @SuppressLint("ClickableViewAccessibility")
    private fun showWidget() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val size = (56 * density).toInt()

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
            y = (resources.displayMetrics.heightPixels * 0.3f).toInt()
        }

        // ═══ سحب + ضغط ═══
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
                        if (isExpanded) {
                            collapseWidget(density)
                        } else {
                            expandWidget(density)
                        }
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
        val size = (56 * density).toInt()

        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        // ═══ الدائرة ═══
        val circle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFFE8C547.toInt())
                setStroke((2 * density).toInt(), 0xFF000000.toInt())
            }
            elevation = 8 * density
        }
        container.addView(circle)

        // ═══ أيقونة ⇄ ═══
        val icon = TextView(this).apply {
            text = "⇄"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(0xFF000000.toInt())
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
        container.addView(icon)

        // ═══ اللوحة الموسّعة (مخفية) ═══
        val panel = createExpandedPanel(density)
        panel.visibility = View.GONE
        panel.tag = "expanded"
        container.addView(panel)

        return container
    }

    private fun createExpandedPanel(density: Float): LinearLayout {
        val panelW = (260 * density).toInt()
        val pad = (10 * density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            layoutParams = FrameLayout.LayoutParams(
                panelW,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = (60 * density).toInt()
            }

            background = GradientDrawable().apply {
                cornerRadius = 12 * density
                setColor(0xDD1A1A1A.toInt())
                setStroke((1 * density).toInt(), 0xFFE8C547.toInt())
            }
            elevation = 10 * density

            // ═══ App 1 ═══
            addView(createAppButton(
                getAppName(app1Package), 0xFFE8C547.toInt(), density
            ) {
                launchApp1()
                collapseWidget(density)
            })

            addView(createSeparator(density))

            // ═══ App 2 ═══
            addView(createAppButton(
                getAppName(app2Package), 0xFF4CAF50.toInt(), density
            ) {
                switchToApp2()
                collapseWidget(density)
            })

            addView(createSeparator(density))

            // ═══ أزرار التحكم ═══
            val controls = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, (4 * density).toInt(), 0, 0)
            }

            controls.addView(createControlBtn("−", 0xFFFFC107.toInt(), density) {
                floatingMgr.minimize()
                collapseWidget(density)
            })

            controls.addView(createControlBtn("□", 0xFF2196F3.toInt(), density) {
                floatingMgr.maximize()
                collapseWidget(density)
            })

            controls.addView(createControlBtn("R", 0xFF9C27B0.toInt(), density) {
                floatingMgr.restore()
                collapseWidget(density)
            })

            controls.addView(createControlBtn("✕", 0xFFF44336.toInt(), density) {
                stopEverything()
                stopSelf()
            })

            addView(controls)
        }
    }

    private fun createAppButton(
        text: String,
        color: Int,
        density: Float,
        onClick: () -> Unit
    ): TextView {
        val pad = (12 * density).toInt()

        return TextView(this).apply {
            this.text = "▶ $text"
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, (10 * density).toInt(), pad, (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * density).toInt()
            )

            background = GradientDrawable().apply {
                cornerRadius = 8 * density
                setColor(0x00000000)
            }

            setOnClickListener { onClick() }

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        (v.background as? GradientDrawable)?.setColor(0x22FFFFFF)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        (v.background as? GradientDrawable)?.setColor(0x00000000)
                    }
                }
                false
            }
        }
    }

    private fun createControlBtn(
        text: String,
        color: Int,
        density: Float,
        onClick: () -> Unit
    ): TextView {
        val size = (36 * density).toInt()
        val margin = (4 * density).toInt()

        return TextView(this).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = margin
                marginEnd = margin
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x33FFFFFF)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createSeparator(density: Float): View {
        return View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt()
            ).apply {
                marginStart = (12 * density).toInt()
                marginEnd = (12 * density).toInt()
                topMargin = (4 * density).toInt()
                bottomMargin = (4 * density).toInt()
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
        params.width = (320 * density).toInt()
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
        val size = (56 * density).toInt()
        params.width = size
        params.height = size
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        try {
            windowManager?.updateViewLayout(widgetView, params)
        } catch (_: Exception) {}
    }

    // ═══════════════════════════════════════════
    // مساعدة
    // ═══════════════════════════════════════════
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
        handler.removeCallbacks(relaunchRunnable)
        floatingMgr.close()
        removeWidget()
    }

    private fun removeWidget() {
        widgetView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        widgetView = null
    }

    // ═══════════════════════════════════════════
    // إشعارات
    // ═══════════════════════════════════════════
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
