package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SkyBlue = Color(0xFF38BDF8)
val CheerfulPurple = Color(0xFFA855F7)
val WarmGold = Color(0xFFFBBF24)
val LimeGreen = Color(0xFF22C55E)
val CoralRed = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Color(0xFF0F172A),
    secondary = CheerfulPurple,
    onSecondary = Color.White,
    tertiary = WarmGold,
    onTertiary = Color(0xFF0F172A),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569),
    error = CoralRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF7E22CE),
    onSecondary = Color.White,
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
