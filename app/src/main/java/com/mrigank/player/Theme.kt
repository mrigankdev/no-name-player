package com.mrigank.player

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class Pal(
    val name: String,
    val bg: Color,
    val surface: Color,
    val onBg: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val light: Boolean
)

private fun darkPal(name: String, bg: Long, surface: Long, accent: Long) = Pal(
    name = name,
    bg = Color(bg),
    surface = Color(surface),
    onBg = Color(0xFFF2F3F5),
    muted = Color(0xFF9AA0A6),
    accent = Color(accent),
    onAccent = Color(0xFF0E0F11),
    light = false
)

private fun lightPal(name: String, bg: Long, surface: Long, accent: Long) = Pal(
    name = name,
    bg = Color(bg),
    surface = Color(surface),
    onBg = Color(0xFF1B1C1E),
    muted = Color(0xFF6A6E75),
    accent = Color(accent),
    onAccent = Color(0xFFFFFFFF),
    light = true
)

val palettes: List<Pal> = listOf(
    darkPal("Graphite", 0xFF0B0B0C, 0xFF1A1A1D, 0xFFE8EAED),
    darkPal("Charcoal", 0xFF121212, 0xFF1F1F1F, 0xFFD4D7DC),
    darkPal("Slate", 0xFF0F141A, 0xFF1A222B, 0xFF9DB1C6),
    darkPal("Midnight", 0xFF0A1022, 0xFF141C34, 0xFF7FA8FF),
    darkPal("Forest", 0xFF0A1410, 0xFF13221A, 0xFF78D49E),
    darkPal("Dusk", 0xFF130F1E, 0xFF201A30, 0xFFB9A3FF),
    darkPal("Rust", 0xFF160E0B, 0xFF251915, 0xFFE69A74),
    darkPal("Ink", 0xFF000000, 0xFF0E0E0E, 0xFFFFFFFF),
    lightPal("Paper", 0xFFFAF8F4, 0xFFFFFFFF, 0xFF2B2B2D),
    lightPal("Cloud", 0xFFF3F5F8, 0xFFFFFFFF, 0xFF3B4A5E),
    lightPal("Mist", 0xFFEDF3F1, 0xFFFFFFFF, 0xFF2E6F68),
    lightPal("Linen", 0xFFF5F0E8, 0xFFFFFFFF, 0xFF8A5A3C),
    lightPal("Lilac", 0xFFF2EFFA, 0xFFFFFFFF, 0xFF6B4FB3)
)

val LocalPal = staticCompositionLocalOf { palettes[0] }

@Composable
fun AppTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val base = palettes.firstOrNull { it.name == prefs.theme } ?: palettes[0]
    val pal = if (prefs.pureBlack && !base.light) {
        base.copy(bg = Color.Black, surface = Color(0xFF0B0B0B))
    } else {
        base
    }
    val scheme = if (pal.light) {
        lightColorScheme(
            primary = pal.accent,
            onPrimary = pal.onAccent,
            background = pal.bg,
            onBackground = pal.onBg,
            surface = pal.surface,
            onSurface = pal.onBg
        )
    } else {
        darkColorScheme(
            primary = pal.accent,
            onPrimary = pal.onAccent,
            background = pal.bg,
            onBackground = pal.onBg,
            surface = pal.surface,
            onSurface = pal.onBg
        )
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = pal.light
            controller.isAppearanceLightNavigationBars = pal.light
        }
    }
    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
