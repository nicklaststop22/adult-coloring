package com.example.ui.theme

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
    primary = InkVioletLight,
    onPrimary = InkBlack,
    primaryContainer = InkVioletPrimary,
    onPrimaryContainer = Color.White,
    secondary = InkAmberSecondary,
    onSecondary = InkBlack,
    tertiary = InkCoralTertiary,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC7C3DD),
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = InkVioletPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E4FC),
    onPrimaryContainer = InkVioletPrimary,
    secondary = InkAmberSecondary,
    onSecondary = InkBlack,
    tertiary = InkCoralTertiary,
    background = LightBackground,
    onBackground = InkBlack,
    surface = LightSurface,
    onSurface = InkBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFDDD8ED)
)

@Composable
fun InkZenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted palette for consistent brand identity
    content: @Composable () -> Unit,
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
