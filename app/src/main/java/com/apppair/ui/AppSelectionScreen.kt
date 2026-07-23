package com.apppair.ui

import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectingSlot by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Pair Switcher") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!Settings.canDrawOverlays(context)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Overlay permission required", fontWeight = FontWeight.Bold)
                            Text("Needed for floating switcher", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}"))
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Grant") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text("Select two apps:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSlotCard("App 1", uiState.selectedApp1, selectingSlot == 1, { selectingSlot = 1 }, Modifier.weight(1f))
                AppSlotCard("App 2", uiState.selectedApp2, selectingSlot == 2, { selectingSlot = 2 }, Modifier.weight(1f))
.dp))

            if (uiState.selectedApp1 != null && uiState.selectedApp2 != null) {
                Row            }

            Spacer(Modifier.height(12(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val intent = Intent(context, OverlayService::class.java).apply {
                                putExtra(OverlayService.EXTRA_APP1, uiState.selectedApp1!!.packageName)
                                putExtra(OverlayService.EXTRA_APP2, uiState.selectedApp2!!.packageName)
                            }
                            context.startForegroundService(intent)
                            viewModel.setServiceRunning(true)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isServiceRunning
                    ) { Text("Start") }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, OverlayService::class.java).apply {
                                action = OverlayService.ACTION_STOP
                            }
                            context.startService(intent)
                            viewModel.setServiceRunning(false)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isServiceRunning
                    ) { Text("Stop") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.clearSelection() }, Modifier.fillMaxWidth()) {
                    Text("Clear")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectingSlot > 0) {
                Text("Pick app for Slot $selectingSlot:")
                Spacer(Modifier.height(8.dp))
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error!!, color = MaterialTheme.colorScheme.error) }
                selectingSlot > 0 -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(uiState.installedApps) { app ->
                        AppListItem(app) { when (selectingSlot) { 1 -> viewModel.selectApp1(app); 2 -> viewModel.selectApp2(app) }; selectingSlot = 0 }
                    }
                }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tap App 1 or App 2 above", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun AppSlotCard(label: String, app: InstalledApp?, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.height(100.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (app != null) {
                app.icon?.let { d -> Image(bitmap = d.toBitmap(48, 48, Bitmap.Config.ARGB_8888).asImageBitmap(), contentDescription = app.name, modifier = Modifier.size(40.dp)) }
                Spacer(Modifier.height(4.dp))
                Text(app.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text(label, fontWeight = FontWeight.Bold)
                Text("Tap to select", style = MaterialTheme.typography().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            app.icon?.let { d -> Image(bitmap = d.toBitmap(40, 40, Bitmap.Config.ARGB_8888).asImageBitmap(), contentDescription = app.name, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(app.name)
                Text(app.packageName, style () -> Unit) {
    Card(Modifier.fillMaxWidth.bodySmall)
            }
        }
    }
}

@Composable
fun AppListItem(app: InstalledApp, onClick:().clickable { onClick() }, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
