package com.apppair.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF81C784),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003300),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1B5E20),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFA5D6A7),
    secondary = androidx.compose.ui.graphics.Color(0xFF90CAF9),
    surface = androidx.compose.ui.graphics.Color(0xFF0D0D0D),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2E7D32),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFA5D6A7),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF003300)
)

@Composable
fun AppPairTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
