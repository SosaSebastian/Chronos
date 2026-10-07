package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeniorDarkColorScheme = darkColorScheme(
    primary = MintInteractive,
    onPrimary = NavyDark,
    primaryContainer = NavySurfaceVariant,
    onPrimaryContainer = MintInteractive,
    secondary = MintLight,
    onSecondary = NavyDark,
    background = NavyDark,
    onBackground = TextWhite,
    surface = NavySurface,
    onSurface = TextWhite,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextLightBlue,
    outline = MintInteractive,
    error = StatusUrgent,
    onError = TextWhite
)

private val SeniorLightColorScheme = lightColorScheme(
    primary = MintDark,
    onPrimary = TextWhite,
    primaryContainer = Color(0xFFE8FBF8),
    onPrimaryContainer = NavyDark,
    secondary = Color(0xFF0F4C81),
    onSecondary = TextWhite,
    background = Color(0xFFF4F7FC),
    onBackground = NavyDark,
    surface = TextWhite,
    onSurface = NavyDark,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = MintDark,
    error = StatusUrgent,
    onError = TextWhite
)

@Composable
fun HealtyChronosTheme(
    darkTheme: Boolean = true, // Default to Dark Navy Blue aesthetic requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SeniorDarkColorScheme else SeniorLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Alias for compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) = HealtyChronosTheme(darkTheme = darkTheme, content = content)
