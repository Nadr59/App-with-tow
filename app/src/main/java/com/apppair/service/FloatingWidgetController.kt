package com.apppair.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.apppair.data.model.SelectedAppPair
import com.apppair.data.preferences.AppPairPreferences
import com.apppair.ui.MainActivity
import com.apppair.utils.PackageUtils
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class FloatingWidgetController @Inject constructor(
    private val preferences: AppPairPreferences,
    private val appSwitchHelper: AppSwitchHelper
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isShowing = false

    init {
        savedStateRegistryController.performRestore(null)
    }

    fun startWidget(context: Context) {
        if (isShowing) {
            refreshWidget(context)
            return
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Fetch saved position
        CoroutineScope(Dispatchers.Main).launch {
            val selection = preferences.selectedAppPairFlow.firstOrNull()
            val startX = selection?.widgetX ?: 100
            val startY = selection?.widgetY ?: 300

            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = startX
                y = startY
            }

            composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(this@FloatingWidgetController)
                setViewTreeViewModelStoreOwner(this@FloatingWidgetController)
                setViewTreeSavedStateRegistryOwner(this@FloatingWidgetController)

                setContent {
                    FloatingWidgetUI(context = context)
                }
            }

            try {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                windowManager?.addView(composeView, layoutParams)
                isShowing = true
            } catch (e: Exception) {
                e.printStackTrace()
                isShowing = false
            }
        }
    }

    fun stopWidget() {
        if (!isShowing || composeView == null) return
        try {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            windowManager?.removeView(composeView)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            composeView = null
            isShowing = false
            store.clear()
        }
    }

    fun resetPosition(context: Context) {
        if (!isShowing || composeView == null || layoutParams == null) return
        CoroutineScope(Dispatchers.Main).launch {
            layoutParams?.x = 100
            layoutParams?.y = 300
            try {
                windowManager?.updateViewLayout(composeView, layoutParams)
                preferences.saveWidgetPosition(100, 300)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshWidget(context: Context) {
        // Recompose or verify layout
        composeView?.invalidate()
    }

    private fun updateLayoutPosition(x: Int, y: Int) {
        if (!isShowing || composeView == null || layoutParams == null) return
        layoutParams?.x = x
        layoutParams?.y = y
        try {
            windowManager?.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    private fun FloatingWidgetUI(context: Context) {
        val selection by preferences.selectedAppPairFlow.collectAsState(initial = SelectedAppPair())
        val coroutineScope = rememberCoroutineScope()

        val appInfoA = remember(selection.packageA) {
            selection.packageA?.let { PackageUtils.getAppInfo(context, it) }
        }
        val appInfoB = remember(selection.packageB) {
            selection.packageB?.let { PackageUtils.getAppInfo(context, it) }
        }

        var showControlPanel by remember { mutableStateOf(false) }

        // Touch dragging state
        var initialDragX by remember { mutableStateOf(0) }
        var initialDragY by remember { mutableStateOf(0) }
        var isDragging by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            initialDragX = layoutParams?.x ?: 0
                            initialDragY = layoutParams?.y ?: 0
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            val currentX = layoutParams?.x ?: 0
                            val currentY = layoutParams?.y ?: 0
                            coroutineScope.launch {
                                preferences.saveWidgetPosition(currentX, currentY)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = (layoutParams?.x ?: 0) + dragAmount.x.toInt()
                            val newY = (layoutParams?.y ?: 0) + dragAmount.y.toInt()
                            updateLayoutPosition(newX, newY)
                        }
                    )
                }
        ) {
            if (selection.isMinimized) {
                // Minimized state: Collapsed to tiny dot
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                preferences.setWidgetMinimized(false)
                            }
                        },
                    color = Color(0xFF1E293B),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF60A5FA))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp)
                    ) {
                        // Show overlapping mini rings or expand icon
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Expand AppPair",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // Expanded dual-tab state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    Surface(
                        modifier = Modifier
                            .shadow(12.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        showControlPanel = !showControlPanel
                                    }
                                )
                            },
                        color = Color(0xEE0F172A), // Semi-transparent sleek dark blur
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tab A
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, Color(0xFF60A5FA), CircleShape)
                                    .clickable {
                                        selection.packageA?.let { pkg ->
                                            appSwitchHelper.switchToApp(context, pkg)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                appInfoA?.iconDrawable?.let { drawable ->
                                    Image(
                                        painter = rememberDrawablePainter(drawable),
                                        contentDescription = appInfoA.label,
                                        modifier = Modifier.size(34.dp)
                                    )
                                } ?: Text("A", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // Divider/Indicator
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(26.dp)
                                    .background(Color(0xFF334155))
                            )

                            // Tab B
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, Color(0xFFF43F5E), CircleShape)
                                    .clickable {
                                        selection.packageB?.let { pkg ->
                                            appSwitchHelper.switchToApp(context, pkg)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                appInfoB?.iconDrawable?.let { drawable ->
                                    Image(
                                        painter = rememberDrawablePainter(drawable),
                                        contentDescription = appInfoB.label,
                                        modifier = Modifier.size(34.dp)
                                    )
                                } ?: Text("B", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // Control toggle (3-dots)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { showControlPanel = !showControlPanel },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Control Panel (Overlay when long-pressed or more button tapped)
                    AnimatedVisibility(visible = showControlPanel) {
                        Surface(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .shadow(10.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp)),
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                // Change Apps
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showControlPanel = false
                                            val intent = Intent(context, MainActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            }
                                            context.startActivity(intent)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Change apps", color = Color.White, fontSize = 13.sp)
                                }

                                // Minimize Widget
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showControlPanel = false
                                            coroutineScope.launch {
                                                preferences.setWidgetMinimized(true)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Minimize,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Minimize widget", color = Color.White, fontSize = 13.sp)
                                }

                                // Stop Service
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showControlPanel = false
                                            val stopIntent = Intent(context, AppPairForegroundService::class.java).apply {
                                                action = NotificationHelper.ACTION_STOP_SERVICE
                                            }
                                            context.startService(stopIntent)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Stop switcher", color = Color(0xFFF43F5E), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
