package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HologramCyan,
    secondary = NeonGreen,
    tertiary = AlienCrimson,
    background = CyberDark,
    surface = InterfaceCard,
    onPrimary = CyberDark,
    onSecondary = CyberDark,
    onBackground = LightAccent,
    onSurface = LightAccent
)

private val LightColorScheme = lightColorScheme(
    primary = HologramCyan,
    secondary = NeonGreen,
    tertiary = AlienCrimson,
    background = CoolSlate,
    surface = InterfaceCard,
    onPrimary = CyberDark,
    onSecondary = CyberDark,
    onBackground = LightAccent,
    onSurface = LightAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors by default to preserve custom space theme colors
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
