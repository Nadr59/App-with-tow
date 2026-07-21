package com.apppair.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apppair.ui.guide.OemBatteryGuideScreen
import com.apppair.ui.permissions.PermissionsScreen
import com.apppair.ui.selection.AppSelectionScreen
import com.apppair.utils.PermissionUtils

object Screen {
    const val PERMISSIONS = "permissions"
    const val SELECTION = "selection"
    const val OEM_GUIDE = "oem_guide"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val startDestination = remember {
        val status = PermissionUtils.checkPermissions(context)
        if (status.areAllRequiredPermissionsGranted) Screen.SELECTION else Screen.PERMISSIONS
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.PERMISSIONS) {
            PermissionsScreen(
                onNavigateToSelection = {
                    navController.navigate(Screen.SELECTION) {
                        popUpTo(Screen.PERMISSIONS) { inclusive = true }
                    }
                },
                onNavigateToOemGuide = {
                    navController.navigate(Screen.OEM_GUIDE)
                }
            )
        }

        composable(Screen.SELECTION) {
            AppSelectionScreen(
                onNavigateToOemGuide = {
                    navController.navigate(Screen.OEM_GUIDE)
                }
            )
        }

        composable(Screen.OEM_GUIDE) {
            OemBatteryGuideScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
