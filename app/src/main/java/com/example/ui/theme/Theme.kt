package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HealthyChronosDarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = TextWhite,
    primaryContainer = BrandSurfaceVariant,
    onPrimaryContainer = BrandGreenAccent,
    secondary = BrandGreenLight,
    onSecondary = BrandDark,
    background = BrandDark,
    onBackground = TextWhite,
    surface = BrandSurface,
    onSurface = TextWhite,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = TextLightGreen,
    outline = BrandGreenBorder,
    error = StatusUrgent,
    onError = TextWhite
)

private val HealthyChronosLightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = TextWhite,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandBlack,
    secondary = BrandGreenDark,
    onSecondary = TextWhite,
    background = Color(0xFFF7FAF7),
    onBackground = BrandBlack,
    surface = TextWhite,
    onSurface = BrandBlack,
    surfaceVariant = Color(0xFFE5EFE6),
    onSurfaceVariant = Color(0xFF2A3B2D),
    outline = BrandGreen,
    error = StatusUrgent,
    onError = TextWhite
)

@Composable
fun HealtyChronosTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HealthyChronosDarkColorScheme else HealthyChronosLightColorScheme

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
