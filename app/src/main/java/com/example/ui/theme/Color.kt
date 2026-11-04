package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// HEALTHY CHRONOS - PALETA OFICIAL DE MARCA (Basada en el Logo Oficial)
// =========================================================================

// Verde Salud Oficial (Extraído directamente del isotipo y tipografía 'CHRONOS')
val BrandGreen = Color(0xFF22A72A)          // Verde esmeralda de salud (#22A72A)
val BrandGreenDark = Color(0xFF198620)      // Verde oscuro para estados presionados
val BrandGreenLight = Color(0xFFE8F7E9)     // Verde claro suave para fondos y pastillas
val BrandGreenAccent = Color(0xFF34D141)    // Acento verde brillante para indicadores
val BrandGreenBorder = Color(0xFF2FB238)    // Bordes de tarjetas y contrastes

// Negro & Carbón Oficial (Extraído de la tipografía 'HEALTHY')
val BrandBlack = Color(0xFF111111)          // Negro puro de la marca
val BrandDark = Color(0xFF0E1711)           // Fondo oscuro con matiz carbón-bosque profundo
val BrandSurface = Color(0xFF162319)        // Superficie de tarjetas y contenedores
val BrandSurfaceVariant = Color(0xFF1E3123) // Superficies secundarias y divisores
val BrandSurfaceElevated = Color(0xFF253C2B)// Superficies elevadas y banners destacados

// Tipografía y Textos de Alto Contraste (Accesibilidad AAA para adultos mayores)
val TextWhite = Color(0xFFFFFFFF)           // Blanco puro (máximo contraste sobre verde/oscuro)
val TextLight = Color(0xFFEAEAEA)           // Blanco humo legible
val TextLightGreen = Color(0xFFD2EED5)      // Texto secundario legible con matiz de marca
val TextMuted = Color(0xFF8FA992)           // Texto terciario y placeholders
val TextDark = Color(0xFF111813)            // Texto oscuro sobre fondos claros

// Estados Clínicos y Recordatorios
val StatusSuccess = BrandGreen              // Dosis tomada a tiempo (Verde de marca)
val StatusPending = Color(0xFFFF9F1C)       // Dosis pendiente (Ámbar cálido de alto contraste)
val StatusWarning = StatusPending           // Advertencias o tomas próximas
val StatusUrgent = Color(0xFFE63946)        // Contacto de emergencia o síntomas de alerta

// Alias de Compatibilidad para vistas existentes
val NavyDark = BrandDark
val NavySurface = BrandSurface
val NavySurfaceVariant = BrandSurfaceVariant
val NavySurfaceElevated = BrandSurfaceElevated
val MintInteractive = BrandGreen
val MintDark = BrandGreenDark
val MintLight = BrandGreenLight
val TextLightBlue = TextLightGreen
