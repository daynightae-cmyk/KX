package com.kx.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Glass token bundle handed to the brand primitives.
 *
 * Material 3 colour roles alone cannot express "translucent panel with a gradient
 * rim", so the glass-specific values travel beside them instead of being hard-coded
 * inside each widget.
 */
@Immutable
data class KXGlassScheme(
    val background: Color,
    val surface: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorder: Color,
    val innerHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val orbCore: Color,
    val orbMid: Color,
    val orbEdge: Color,
    val rimStops: List<Color>,
    val nodeColors: List<Color>,
    val wordmarkBrush: Brush,
    /**
     * Corner depth for the atmospheric backdrop. Night uses a near-black navy; day must
     * NOT, because 40% black over the pale lilac surface mixes into a grey smudge — the
     * exact defect the first API 35 device screenshot showed. Day deepens toward violet
     * instead, which reads as shadow without going muddy.
     */
    val atmosphereVignette: Color,

    /**
     * Colour for a measured latency. Night can use the brand cyan; day needs a deep blue
     * or the number disappears into the pale surface.
     */
    val latency: Color,
) {
    val glassFillBrush: Brush
        get() = Brush.linearGradient(listOf(glassTop, glassBottom))

    val rimBrush: Brush
        get() = Brush.sweepGradient(rimStops)

    val nodeBrushCacheKey: Int
        get() = nodeColors.hashCode()
}

private val NightGlass = KXGlassScheme(
    background = KXPalette.Midnight,
    surface = KXPalette.Ink,
    glassTop = Color(0x3D6C52FB),
    glassBottom = Color(0x14171B4C),
    glassBorder = Color(0x59C2DAFD),
    innerHighlight = Color(0x2EE7EFFF),
    textPrimary = KXPalette.NightTextPrimary,
    textSecondary = KXPalette.NightTextSecondary,
    accent = KXPalette.Cyan,
    orbCore = KXPalette.OrbGlow,
    orbMid = KXPalette.OrbLavender,
    orbEdge = KXPalette.ShieldDeep,
    rimStops = listOf(
        KXPalette.OrbRimCyan,
        KXPalette.Violet,
        KXPalette.Orchid,
        KXPalette.Cyan,
        KXPalette.OrbRimCyan,
    ),
    nodeColors = listOf(
        KXPalette.NodeBlue,
        KXPalette.NodeOrchid,
        KXPalette.NodePearl,
    ),
    wordmarkBrush = Brush.linearGradient(
        listOf(KXPalette.WordmarkInk, KXPalette.Violet, KXPalette.Cyan),
    ),
    atmosphereVignette = Color(0x70050A24),
    latency = KXPalette.Cyan,
)

private val DayGlass = KXGlassScheme(
    background = KXPalette.DaySurface,
    surface = KXPalette.DaySurfaceHigh,
    glassTop = Color(0x80FFFFFF),
    glassBottom = Color(0x4DD9D4F7),
    glassBorder = Color(0x806537E8),
    innerHighlight = Color(0x66FFFFFF),
    textPrimary = KXPalette.DayTextPrimary,
    textSecondary = KXPalette.DayTextSecondary,
    accent = KXPalette.Violet,
    // On the pale surface the orb keeps its lavender body but the core is pulled
    // down in luminance so the emblem never becomes white-on-white.
    orbCore = Color(0xFFEDE9FF),
    orbMid = KXPalette.OrbLavender,
    orbEdge = Color(0xFF8A6BF2),
    rimStops = listOf(
        Color(0xFF9BC7F5),
        KXPalette.Violet,
        KXPalette.Orchid,
        Color(0xFF4FB8EE),
        Color(0xFF9BC7F5),
    ),
    nodeColors = listOf(
        Color(0xFF4E93F0),
        Color(0xFFB44CE6),
        Color(0xFF6E8FF5),
    ),
    wordmarkBrush = Brush.linearGradient(
        listOf(KXPalette.WordmarkInk, KXPalette.Violet, Color(0xFF2F8FD8)),
    ),
    atmosphereVignette = Color(0x336537E8),
    latency = KXPalette.LatencyDay,
)

val LocalKXGlass: ProvidableCompositionLocal<KXGlassScheme> =
    staticCompositionLocalOf { NightGlass }

@Composable
fun KXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val glass = if (darkTheme) NightGlass else DayGlass

    MaterialTheme(
        colorScheme = if (darkTheme) {
            darkColorScheme(
                primary = glass.accent,
                secondary = KXPalette.Orchid,
                tertiary = KXPalette.Cyan,
                background = glass.background,
                surface = glass.surface,
                onBackground = glass.textPrimary,
                onSurface = glass.textPrimary,
                onSurfaceVariant = glass.textSecondary,
                outline = glass.glassBorder,
            )
        } else {
            lightColorScheme(
                primary = glass.accent,
                secondary = KXPalette.Orchid,
                tertiary = KXPalette.Cyan,
                background = glass.background,
                surface = glass.surface,
                onBackground = glass.textPrimary,
                onSurface = glass.textPrimary,
                onSurfaceVariant = glass.textSecondary,
                outline = glass.glassBorder,
            )
        },
        typography = KXMaterialTypography,
        content = {
            CompositionLocalProvider(
                LocalKXGlass provides glass,
                content = content,
            )
        },
    )
}
