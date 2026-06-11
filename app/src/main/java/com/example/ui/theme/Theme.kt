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
    primary = RioTealDark,
    secondary = RioOceanDark,
    tertiary = RioAmberDark,
    background = RioBgDark,
    surface = RioSurfaceDark,
    onBackground = RioOnBgDark,
    onSurface = RioOnBgDark,
    surfaceVariant = RioSurfaceDark,
    onSurfaceVariant = RioOnBgDark
)

private val LightColorScheme = lightColorScheme(
    primary = RioTealLight,
    secondary = RioOceanLight,
    tertiary = RioAmberLight,
    background = RioBgLight,
    surface = RioSurfaceLight,
    onBackground = RioOnBgLight,
    onSurface = RioOnBgLight,
    surfaceVariant = RioSurfaceLight,
    onSurfaceVariant = RioOnBgLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false so our brand's beautiful Rio colors are primary
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
