package com.assem.sonar.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.assem.sonar.util.LocaleHelper

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7ED9A8),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF005231),
    onPrimaryContainer = Color(0xFF9BF6C4),
    secondary = Color(0xFF9FD4FF),
    onSecondary = Color(0xFF003353),
    tertiary = Color(0xFFFFD08A),
    onTertiary = Color(0xFF3F2E00),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE2E4E9),
    surface = Color(0xFF15181E),
    onSurface = Color(0xFFE2E4E9),
    surfaceVariant = Color(0xFF232A34),
    onSurfaceVariant = Color(0xFFC0C7D2),
    outline = Color(0xFF5A626E),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006D42),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9BF6C4),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF00639B),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D),
)

@Composable
fun SonarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val context = LocalContext.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
        }
    }
    val direction = if (LocaleHelper.isRtl(context)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
