package com.apppair.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apppair.ui.theme.AppPairTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppPairTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
fun MainContent() {
    val ctx = LocalContext.current
    var showGuide by remember { mutableStateOf(isMiuiDevice()) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showGuide) {
            OemOptimizationGuide(onDone = { showGuide = false })
        } else {
            AppSelectionScreen()
        }
    }
}

private fun isMiuiDevice(): Boolean {
    val m = Build.MANUFACTURER.lowercase()
    return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")
}

@Composable
fun OemOptimizationGuide(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var step by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "One-Time Setup Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Your ${Build.MANUFACTURER} device kills background apps.\nComplete these steps once to fix it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        // ═══ الخطوة 1: تحسين البطارية ═══
        StepCard(
            number = 1,
            title = "Disable Battery Optimization",
            description = "Tap below → Find 'AppPair' → Select 'No restrictions'",
            isDone = step > 0,
            buttonText = "Open Battery Settings"
        ) {
            try {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
                    ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${ctx.packageName}")
                        }
                    )
                }
            } catch (_: Exception) {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                } catch (_: Exception) {}
            }
            step = 1
        }

        // ═══ الخطوة 2: التشغيل التلقائي (MIUI) ═══
        StepCard(
            number = 2,
            title = "Enable Auto-Start",
            description = "Tap below → Enable 'AutoStart' for AppPair",
            isDone = step > 1,
            buttonText = "Open Auto-Start"
        ) {
            openMiuiAutoStart(ctx)
            step = 2
        }

        // ═══ الخطوة 3: إعدادات MIUI Battery ═══
        StepCard(
            number = 3,
            title = "MIUI Battery Saver → OFF",
            description = "Tap below → Set AppPair to 'No restrictions'",
            isDone = step > 2,
            buttonText = "Open MIUI Battery"
        ) {
            openMiuiBattery(ctx)
            step = 3
        }

        // ═══ الخطوة 4: إعدادات التطبيق ═══
        StepCard(
            number = 4,
            title = "App Info → All Permissions",
            description = "Tap below → Make sure all permissions are allowed",
            isDone = step > 3,
            buttonText = "Open App Settings"
        ) {
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${ctx.packageName}")
                }
            )
            step = 4
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I've done all steps → Continue to App Selection")
        }
    }
}

@Composable
fun StepCard(
    number: Int,
    title: String,
    description: String,
    isDone: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isDone) "✅ Step $number: $title" else "Step $number: $title",
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(buttonText)
            }
        }
    }
}

// ═══════════════════════════════════════════
// فتح إعدادات MIUI
// ═══════════════════════════════════════════
private fun openMiuiAutoStart(ctx: Context) {
    val intents = listOf(
        Intent().setComponent(ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )),
        Intent().setComponent(ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.permissions.PermissionsEditorActivity"
        )),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${ctx.packageName}")
        }
    )
    for (intent in intents) {
        try {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {}
    }
}

private fun openMiuiBattery(ctx: Context) {
    val intents = listOf(
        Intent().setComponent(ComponentName(
            "com.miui.securitycenter",
            "com.miui.powercenter.PowerSettings"
        )),
        Intent().setComponent(ComponentName(
            "com.miui.securitycenter",
            "com.miui.powercenter.PowerMainActivity"
        )),
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
    )
    for (intent in intents) {
        try {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {}
    }
}
