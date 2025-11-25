package com.kjjejones42.audiobookplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val darkTheme = darkColorScheme(
    primary = primaryDark,
    primaryContainer = colorAccent,
    surface = primaryDark
)
private val lightTheme = lightColorScheme(
    primary = primary,
    primaryContainer = colorAccent,
    surface = primary
)

@Composable
fun AudiobookPlayerTheme(
    inDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (inDarkTheme) darkTheme else lightTheme
    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        val window = (view.context as Activity).window
        DisposableEffect(inDarkTheme) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !inDarkTheme
            onDispose {}
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}