package com.excalibur.routines.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricVioletLight,
    onPrimary = Color.Black,
    primaryContainer = ElectricViolet,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D5A),
    onSecondaryContainer = NeonCyan,
    tertiary = SunsetCoral,
    onTertiary = Color.White,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Slate800
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = ElectricViolet,
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF006064),
    tertiary = SunsetCoral,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = LightSurface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFE7E0EC)
)

@Composable
fun RoutinesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set default to false to showcase our custom vibrant theme!
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
