package com.openscan.scanner.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Orange,
    onPrimary = PaperWhite,
    secondary = Ink,
    onSecondary = PaperWhite,
    // Selection / subtle highlight: a faint wash of the accent.
    secondaryContainer = Orange.copy(alpha = 0.12f),
    onSecondaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = InkSoft,
    outline = Hairline,
    outlineVariant = Hairline,
    error = DangerRed,
    onError = PaperWhite
)

private val DarkColors = darkColorScheme(
    primary = Orange,
    onPrimary = PaperWhite,
    secondary = PaperWhite,
    onSecondary = InkBlack,
    secondaryContainer = Orange.copy(alpha = 0.22f),
    onSecondaryContainer = PaperWhite,
    background = InkBlack,
    onBackground = PaperWhite,
    surface = InkSurfaceDark,
    onSurface = PaperWhite,
    surfaceVariant = InkSurfaceDark,
    onSurfaceVariant = InkSoftDark,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    error = Orange,
    onError = InkBlack
)

@Composable
fun OpenScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // The masthead is always the inverse (ink) block, so the status bar is ink.
            window.statusBarColor = InkBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
