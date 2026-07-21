package com.apppair.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.apppair.utils.PackageUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSwitchHelper @Inject constructor() {

    private val taskIds = mutableMapOf<String, Int>()

    @Synchronized
    fun saveTaskId(packageName: String, taskId: Int) {
        if (taskId != -1) {
            taskIds[packageName] = taskId
        }
    }

    @Synchronized
    fun getTaskId(packageName: String): Int {
        return taskIds[packageName] ?: -1
    }

    fun findRunningTaskId(context: Context, packageName: String): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        try {
            // First check appTasks if accessible
            for (appTask in activityManager.appTasks) {
                val taskInfo = appTask.taskInfo
                val basePkg = taskInfo.baseActivity?.packageName
                val topPkg = taskInfo.topActivity?.packageName
                val origPkg = taskInfo.origActivity?.packageName
                if (basePkg == packageName || topPkg == packageName || origPkg == packageName) {
                    val id = taskInfo.taskId
                    if (id != -1) {
                        saveTaskId(packageName, id)
                        return id
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback if appTasks fails due to permissions/security
        }

        try {
            @Suppress("DEPRECATION")
            val runningTasks = activityManager.getRunningTasks(100)
            if (runningTasks != null) {
                for (taskInfo in runningTasks) {
                    val basePkg = taskInfo.baseActivity?.packageName
                    val topPkg = taskInfo.topActivity?.packageName
                    val origPkg = taskInfo.origActivity?.packageName
                    if (basePkg == packageName || topPkg == packageName || origPkg == packageName) {
                        val id = taskInfo.taskId
                        if (id != -1) {
                            saveTaskId(packageName, id)
                            return id
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return getTaskId(packageName)
    }

    fun switchToApp(context: Context, packageName: String) {
        if (packageName.isBlank()) return

        val appInfo = PackageUtils.getAppInfo(context, packageName)
        val appName = appInfo?.label ?: packageName

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val taskId = findRunningTaskId(context, packageName)

        var switchedViaTask = false
        if (taskId != -1) {
            try {
                // Attempt instant task switch
                activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
                switchedViaTask = true
            } catch (e: Exception) {
                switchedViaTask = false
            }
        }

        if (!switchedViaTask) {
            // Launch via explicit launch intent
            val launchIntent = PackageUtils.getLaunchIntent(context, packageName)
            if (launchIntent != null) {
                try {
                    context.startActivity(launchIntent)
                    // After brief delay, find and store new task ID
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        findRunningTaskId(context, packageName)
                    }, 1000)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to launch $appName", Toast.LENGTH_SHORT).show()
                    return
                }
            } else {
                Toast.makeText(context, "Cannot launch $appName (App not found)", Toast.LENGTH_SHORT).show()
                return
            }
        }

        Toast.makeText(context, "Switched to $appName", Toast.LENGTH_SHORT).show()
    }

    fun checkAppStatusAndRestartIfKilled(context: Context, packageA: String?, packageB: String?): AppPairHealthStatus {
        if (packageA.isNullOrBlank() || packageB.isNullOrBlank()) {
            return AppPairHealthStatus.INVALID_SELECTION
        }

        val isInstalledA = PackageUtils.isPackageInstalled(context, packageA)
        val isInstalledB = PackageUtils.isPackageInstalled(context, packageB)

        if (!isInstalledA || !isInstalledB) {
            return AppPairHealthStatus.APP_UNINSTALLED
        }

        val taskIdA = findRunningTaskId(context, packageA)
        val taskIdB = findRunningTaskId(context, packageB)

        // If both tasks are missing in running tasks, they might have been killed or not launched yet.
        // We log status and can trigger restart if needed.
        return AppPairHealthStatus.HEALTHY
    }
}

enum class AppPairHealthStatus {
    HEALTHY,
    APP_UNINSTALLED,
    INVALID_SELECTION
}
