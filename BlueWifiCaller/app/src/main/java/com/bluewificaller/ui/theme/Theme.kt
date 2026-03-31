package com.bluewificaller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── Colors ──────────────────────────────────────────────────────────────────
val Midnight    = Color(0xFF0A0F1E)
val DeepNavy    = Color(0xFF0D1B2A)
val ElectricBlue = Color(0xFF00B4D8)
val CyanGlow    = Color(0xFF48CAE4)
val NeonGreen   = Color(0xFF06D6A0)
val AlertRed    = Color(0xFFEF233C)
val AmberWarm   = Color(0xFFFFA62B)
val Surface1    = Color(0xFF111827)
val Surface2    = Color(0xFF1F2B3A)
val Surface3    = Color(0xFF243447)
val OnSurface   = Color(0xFFE2EAF0)
val Subdued     = Color(0xFF6B8FAF)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Midnight,
    primaryContainer = Surface2,
    onPrimaryContainer = CyanGlow,
    secondary = NeonGreen,
    onSecondary = Midnight,
    tertiary = AmberWarm,
    background = Midnight,
    onBackground = OnSurface,
    surface = Surface1,
    onSurface = OnSurface,
    surfaceVariant = Surface2,
    onSurfaceVariant = Subdued,
    error = AlertRed,
    outline = Surface3
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006494),
    onPrimary = Color.White,
    secondary = Color(0xFF1B998B),
    background = Color(0xFFF0F5FB),
    surface = Color.White,
    onBackground = Color(0xFF0D1B2A),
    onSurface = Color(0xFF0D1B2A)
)

@Composable
fun BlueWifiCallerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)
