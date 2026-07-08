package com.keepsy.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    background = Background,
    surface = SurfaceSecondary,
    onPrimary = Color(0xFF0B1220),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor,
    error = ErrorRed,
    secondary = TextSecondary,
    surfaceVariant = SurfaceTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    background = BackgroundLight,
    surface = SurfaceSecondaryLight,
    onPrimary = BackgroundLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = BorderColorLight,
    error = ErrorRed,
    secondary = TextSecondaryLight,
    surfaceVariant = SurfaceTertiaryLight
)

@Composable
fun KeepsyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            // Set appearance of status bars and navigation bars
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
