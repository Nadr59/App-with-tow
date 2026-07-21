package com.apppair.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.apppair.data.repository.AppRepository
import com.apppair.ui.navigation.AppNavigation
import com.apppair.ui.theme.AppPairTheme
import com.apppair.utils.PackageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppPairTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndCleanUninstalledApps()
    }

    private fun checkAndCleanUninstalledApps() {
        lifecycleScope.launch {
            val selection = repository.selectedAppPairFlow.firstOrNull() ?: return@launch
            var changed = false
            var packageA = selection.packageA ?: ""
            var packageB = selection.packageB ?: ""

            if (packageA.isNotEmpty() && !PackageUtils.isPackageInstalled(this@MainActivity, packageA)) {
                packageA = ""
                changed = true
            }
            if (packageB.isNotEmpty() && !PackageUtils.isPackageInstalled(this@MainActivity, packageB)) {
                packageB = ""
                changed = true
            }

            if (changed) {
                repository.selectApps(packageA, packageB)
                Toast.makeText(
                    this@MainActivity,
                    "An uninstalled paired app was detected and removed from your selection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
