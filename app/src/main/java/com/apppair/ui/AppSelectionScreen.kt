package com.apppair.ui

import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.apppair.data.repository.InstalledApp
import com.apppair.service.OverlayService

private fun safeBitmap(d: android.graphics.drawable.Drawable?, w: Int, h: Int): Bitmap? = try { d?.toBitmap(w, h, Bitmap.Config.ARGB_8888) } catch (_: Exception) { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(viewModel: MainViewModel = hiltViewModel()) {
    val s by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    var slot by remember { mutableIntStateOf(0) }

    Scaffold(topBar = { TopAppBar(title = { Text("App Pair Switcher") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            if (!Settings.canDrawOverlays(ctx)) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) { Text("Overlay permission required", fontWeight = FontWeight.Bold); Text("Needed for floating switcher", style = MaterialTheme.typography.bodySmall) }
                        Button(onClick = { try { ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${ctx.packageName}"))) } catch (_: Exception) {} }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Grant") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Text("Select two apps:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Slot("App 1", s.selectedApp1, slot == 1, { slot = 1 }, Modifier.weight(1f))
                Slot("App 2", s.selectedApp2, slot == 2, { slot = 2 }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            if (s.selectedApp1 != null && s.selectedApp2 != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { try { ctx.startForegroundService(Intent(ctx, OverlayService::class.java).apply { putExtra(OverlayService.EXTRA_APP1, s.selectedApp1!!.packageName); putExtra(OverlayService.EXTRA_APP2, s.selectedApp2!!.packageName) }); viewModel.setServiceRunning(true) } catch (_: Exception) {} }, Modifier.weight(1f), enabled = !s.isServiceRunning) { Text("Start") }
                    OutlinedButton(onClick = { try { ctx.startService(Intent(ctx, OverlayService::class.java).apply { action = OverlayService.ACTION_STOP }); viewModel.setServiceRunning(false) } catch (_: Exception) {} }, Modifier.weight(1f), enabled = s.isServiceRunning) { Text("Stop") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.clearSelection() }, Modifier.fillMaxWidth()) { Text("Clear") }
            }
            Spacer(Modifier.height(16.dp))
            if (slot > 0) { Text("Pick app for Slot $slot:"); Spacer(Modifier.height(8.dp)) }
            when {
                s.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                s.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                slot > 0 -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) { items(s.installedApps) { a -> AppListItem(a) { when (slot) { 1 -> viewModel.selectApp1(a); 2 -> viewModel.selectApp2(a) }; slot = 0 } } }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tap App 1 or App 2 above", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
fun Slot(label: String, app: InstalledApp?, sel: Boolean, onClick: () -> Unit, mod: Modifier = Modifier) {
    Card(mod.height(100.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (app != null) {
                val bmp = remember(app.packageName) { safeBitmap(app.icon, 48, 48) }
                if (bmp != null) Image(bmp.asImageBitmap(), app.name, Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp)); Text(app.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else { Text(label, fontWeight = FontWeight.Bold); Text("Tap to select", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun AppListItem(app: InstalledApp, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val bmp = remember(app.packageName) { safeBitmap(app.icon, 40, 40) }
            if (bmp != null) Image(bmp.asImageBitmap(), app.name, Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(12.dp)); Column { Text(app.name); Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
