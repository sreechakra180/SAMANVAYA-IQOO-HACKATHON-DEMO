package com.sukshma.samanvaya.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SamanvayaDarkColorScheme = darkColorScheme(
    primary          = SamanvayaColors.AccentPrimary,
    onPrimary        = SamanvayaColors.TextPrimary,
    secondary        = SamanvayaColors.AccentSecondary,
    onSecondary      = SamanvayaColors.TextPrimary,
    tertiary         = SamanvayaColors.AccentSuccess,
    background       = SamanvayaColors.DeepIndigo,
    onBackground     = SamanvayaColors.TextPrimary,
    surface          = SamanvayaColors.SurfaceCard,
    onSurface        = SamanvayaColors.TextPrimary,
    surfaceVariant   = SamanvayaColors.SurfaceElevated,
    onSurfaceVariant = SamanvayaColors.TextSecondary,
    error            = SamanvayaColors.AccentCritical,
    onError          = SamanvayaColors.TextPrimary,
)

@Composable
fun SamanvayaTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SamanvayaDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SamanvayaColors.DeepIndigo.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SamanvayaTypography,
        content     = content
    )
}
