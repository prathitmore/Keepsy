package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DeepIndigoPrimary,
    onPrimary = Color.White,
    secondary = SoftTealSecondary,
    onSecondary = Color.White,
    background = Slate900DarkBackground,
    onBackground = OnSurfaceDarkText,
    surface = Slate800DarkSurface,
    onSurface = OnSurfaceDarkText,
    surfaceVariant = Color(0xFF333038),
    onSurfaceVariant = TextMutedDark,
    error = MutedRedDanger,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = DeepIndigoPrimary,
    onPrimary = Color.White,
    secondary = SoftTealSecondary,
    onSecondary = Color.White,
    background = Slate50LightBackground,
    onBackground = OnSurfaceLightText,
    surface = Color.White,
    onSurface = OnSurfaceLightText,
    surfaceVariant = Slate100LightSurface,
    onSurfaceVariant = TextMutedLight,
    error = MutedRedDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // uses preset typography matching Material 3
        content = content
    )
}
