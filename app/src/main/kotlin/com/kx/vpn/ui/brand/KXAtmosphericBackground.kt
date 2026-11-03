package com.kx.vpn.ui.brand

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.LocalKXGlass
import com.kx.vpn.ui.theme.LocalKXMotion

/**
 * The atmospheric wave backdrop from `assets/reference/01-welcome.png`, drawn as
 * parametric Béziers rather than shipped as a bitmap.
 *
 * A bitmap here would be the "screenshot as UI background" the engineering contract
 * forbids, and it would also cost APK size and memory for something that scales poorly
 * between 360dp and tablets. Geometry scales exactly; a JPEG does not.
 *
 * Under reduced motion the drift phase is frozen — the composition is unchanged, only
 * the movement stops.
 */
@Composable
fun KXAtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val glass = LocalKXGlass.current
    val motion = LocalKXMotion.current

    val drift: Float = if (motion.reduced) {
        0f
    } else {
        rememberInfiniteTransition(label = "kx-atmosphere").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(24_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "drift",
        ).value
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Decorative: announced only so a screen reader can skip it deliberately
            // rather than encountering an unnamed region.
            .semantics { contentDescription = "" },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            drawBaseWash(glass.background, glass.surface, w, h)
            drawUpperGlow(w, h, glass.accent)
            drawWaveBand(
                anchorY = h * 0.62f,
                amplitude = h * 0.05f,
                phase = drift,
                brush = Brush.linearGradient(
                    listOf(
                        Color.Transparent,
                        KXPalette.Violet.copy(alpha = 0.30f),
                        KXPalette.Orchid.copy(alpha = 0.42f),
                        KXPalette.Cyan.copy(alpha = 0.26f),
                        Color.Transparent,
                    ),
                    start = Offset(0f, h * 0.55f),
                    end = Offset(w, h * 0.85f),
                ),
            )
            drawWaveBand(
                anchorY = h * 0.80f,
                amplitude = h * 0.04f,
                phase = -drift,
                brush = Brush.linearGradient(
                    listOf(
                        Color.Transparent,
                        KXPalette.Cyan.copy(alpha = 0.34f),
                        KXPalette.NodeBlue.copy(alpha = 0.30f),
                        KXPalette.Orchid.copy(alpha = 0.30f),
                        Color.Transparent,
                    ),
                    start = Offset(0f, h * 0.75f),
                    end = Offset(w, h),
                ),
            )
            drawRidgeHighlight(h * 0.62f, h * 0.05f, drift, w, h)
            drawRidgeHighlight(h * 0.80f, h * 0.04f, -drift, w, h)
            drawCornerVignette(w, h)
        }
        content()
    }
}

private fun DrawScope.drawBaseWash(background: Color, surface: Color, w: Float, h: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to background,
                0.42f to surface,
                0.78f to background,
                1f to surface,
            ),
        ),
        size = Size(w, h),
    )
}

private fun DrawScope.drawUpperGlow(w: Float, h: Float, accent: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to accent.copy(alpha = 0.20f),
                0.55f to KXPalette.Violet.copy(alpha = 0.10f),
                1f to Color.Transparent,
            ),
            center = Offset(w * 0.5f, h * 0.22f),
            radius = w * 0.75f,
        ),
        radius = w * 0.75f,
        center = Offset(w * 0.5f, h * 0.22f),
    )
}

/** One flowing luminous band across the lower half of the screen. */
private fun DrawScope.drawWaveBand(
    anchorY: Float,
    amplitude: Float,
    phase: Float,
    brush: Brush,
) {
    val w = this.size.width
    val h = this.size.height
    val shift = phase * w * 0.06f

    val band = Path().apply {
        moveTo(-w * 0.1f + shift, anchorY + amplitude)
        cubicTo(
            w * 0.22f + shift, anchorY - amplitude * 2.4f,
            w * 0.62f + shift, anchorY + amplitude * 1.8f,
            w * 1.1f + shift, anchorY - amplitude * 0.6f,
        )
        lineTo(w * 1.1f + shift, h * 1.05f)
        cubicTo(
            w * 0.62f + shift, h * 1.05f,
            w * 0.22f + shift, h * 1.05f,
            -w * 0.1f + shift, h * 1.05f,
        )
        close()
    }
    drawPath(path = band, brush = brush)
}

/** The thin bright edge the reference shows riding on top of each band. */
private fun DrawScope.drawRidgeHighlight(
    anchorY: Float,
    amplitude: Float,
    phase: Float,
    w: Float,
    h: Float,
) {
    val shift = phase * w * 0.06f
    val ridge = Path().apply {
        moveTo(-w * 0.1f + shift, anchorY + amplitude)
        cubicTo(
            w * 0.22f + shift, anchorY - amplitude * 2.4f,
            w * 0.62f + shift, anchorY + amplitude * 1.8f,
            w * 1.1f + shift, anchorY - amplitude * 0.6f,
        )
    }
    drawPath(
        path = ridge,
        brush = Brush.linearGradient(
            listOf(
                Color.Transparent,
                KXPalette.OrbRimCyan.copy(alpha = 0.55f),
                KXPalette.Cyan.copy(alpha = 0.65f),
                Color.Transparent,
            ),
            start = Offset(0f, anchorY),
            end = Offset(w, anchorY),
        ),
        style = Stroke(width = w * 0.006f),
    )
}

private fun DrawScope.drawCornerVignette(w: Float, h: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(0f to Color.Transparent, 1f to Color(0x66000000)),
            center = Offset(w * 0.06f, h * 0.04f),
            radius = w * 0.6f,
        ),
        radius = w * 0.6f,
        center = Offset(w * 0.06f, h * 0.04f),
    )
}
