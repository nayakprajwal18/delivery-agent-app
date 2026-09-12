package com.yourteam.deliveryagent.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = DeepOrange,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = DeepOrangeLight,
    secondary        = Amber,
    onSecondary      = androidx.compose.ui.graphics.Color.Black,
    secondaryContainer = AmberLight,
    background       = SurfaceLight,
    surface          = SurfaceLight,
    onSurfaceVariant = OnSurfaceVariant,
    error            = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    primary          = DeepOrangeLight,
    onPrimary        = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = DeepOrange,
    secondary        = AmberLight,
    onSecondary      = androidx.compose.ui.graphics.Color.Black,
    secondaryContainer = Amber,
    background       = SurfaceDark,
    surface          = SurfaceDark,
    onSurfaceVariant = OnSurfaceVariant,
    error            = ErrorRed,
)

@Composable
fun DeliveryAgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color uses the Android 12+ wallpaper-based palette.
    // Disabled by default so the brand colors are always used in the hackathon demo.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Apply status-bar color to match the theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content,
    )
}
