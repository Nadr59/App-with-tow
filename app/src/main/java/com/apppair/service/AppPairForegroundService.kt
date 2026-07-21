package com.apppair.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.apppair.data.preferences.AppPairPreferences
import com.apppair.data.repository.AppRepository
import com.apppair.ui.MainActivity
import com.apppair.utils.NotificationHelper
import com.apppair.utils.PackageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppPairForegroundService : LifecycleService() {

    @Inject
    lateinit var repository: AppRepository

    @Inject
    lateinit var floatingWidgetController: FloatingWidgetController

    @Inject
    lateinit var appSwitchHelper: AppSwitchHelper

    private var monitoringJob: Job? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            NotificationHelper.ACTION_STOP_SERVICE -> {
                stopAppPairService()
                return START_NOT_STICKY
            }
            NotificationHelper.ACTION_RESET_WIDGET_POSITION -> {
                floatingWidgetController.resetPosition(this)
                return START_STICKY
            }
            NotificationHelper.ACTION_OPEN_SETTINGS -> {
                val openIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(openIntent)
                return START_STICKY
            }
        }

        startForegroundServiceAndWidget()
        return START_STICKY
    }

    private fun startForegroundServiceAndWidget() {
        lifecycleScope.launch {
            val selection = repository.selectedAppPairFlow.firstOrNull()
            val packageA = selection?.packageA ?: ""
            val packageB = selection?.packageB ?: ""

            val infoA = PackageUtils.getAppInfo(this@AppPairForegroundService, packageA)
            val infoB = PackageUtils.getAppInfo(this@AppPairForegroundService, packageB)

            val labelA = infoA?.label ?: packageA
            val labelB = infoB?.label ?: packageB

            val notification = NotificationHelper.buildForegroundNotification(
                this@AppPairForegroundService,
                labelA = labelA,
                labelB = labelB
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NotificationHelper.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NotificationHelper.NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }

            repository.setServiceActive(true)
            floatingWidgetController.startWidget(this@AppPairForegroundService)
            WatchdogAlarmReceiver.scheduleWatchdog(this@AppPairForegroundService)

            startMonitoringLoop(packageA, packageB, labelA, labelB)
        }
    }

    private fun startMonitoringLoop(packageA: String, packageB: String, labelA: String, labelB: String) {
        monitoringJob?.cancel()
        monitoringJob = lifecycleScope.launch {
            while (isActive) {
                val status = appSwitchHelper.checkAppStatusAndRestartIfKilled(
                    this@AppPairForegroundService,
                    packageA,
                    packageB
                )

                if (status == AppPairHealthStatus.APP_UNINSTALLED) {
                    val warningNotification = NotificationHelper.buildForegroundNotification(
                        this@AppPairForegroundService,
                        labelA = labelA,
                        labelB = labelB,
                        warningMessage = "One or both selected apps were uninstalled!"
                    )
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID, warningNotification)
                } else if (status == AppPairHealthStatus.HEALTHY) {
                    // Refresh task IDs if possible
                    appSwitchHelper.findRunningTaskId(this@AppPairForegroundService, packageA)
                    appSwitchHelper.findRunningTaskId(this@AppPairForegroundService, packageB)
                }

                delay(10_000L) // Check every 10 seconds
            }
        }
    }

    private fun stopAppPairService() {
        monitoringJob?.cancel()
        floatingWidgetController.stopWidget()
        WatchdogAlarmReceiver.cancelWatchdog(this)
        lifecycleScope.launch {
            repository.setServiceActive(false)
            stopForeground(true)
            stopSelf()
        }
    }

    override fun onDestroy() {
        monitoringJob?.cancel()
        floatingWidgetController.stopWidget()
        WatchdogAlarmReceiver.cancelWatchdog(this)
        super.onDestroy()
    }
}
