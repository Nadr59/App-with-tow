package com.apppair.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Dark = darkColorScheme(primary = Color(0xFF81C784), surface = Color(0xFF0D0D0D), surfaceVariant = Color(0xFF1A1A1A))
private val Light = lightColorScheme(primary = Color(0xFF2E7D32))

@Composable
fun AppPairTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { val x = LocalContext.current; if (darkTheme) dynamicDarkColorScheme(x) else dynamicLightColorScheme(x) }
        darkTheme -> Dark; else -> Light
    }
    MaterialTheme(colorScheme = c, typography = Typography(), content = content)
}
