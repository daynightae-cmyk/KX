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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXGlassScheme
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass
import com.kx.vpn.ui.theme.LocalKXMotion
import kotlin.math.cos
import kotlin.math.sin

/**
 * The KX identity: a pearl-glass orb containing a ribbon-folded shield, three
 * orbiting jewel nodes, sparkles and the KX wordmark.
 *
 * This is a native Compose drawing. `assets/reference/00-brand-logo.png` is the
 * design input it was derived from and is never displayed as UI.
 *
 * Geometry is normalised against the 1254x1254 artwork and multiplied by the
 * incoming side length, so the same composable is correct at [KXDimens.OrbHero]
 * on the splash and [KXDimens.OrbInline] in a list row.
 */
object KXOrbTags {
    /**
     * Single source of truth for the emblem's accessibility label. Tests assert on
     * it rather than re-typing the string, so renaming it fails the build instead of
     * silently breaking TalkBack and the instrumented suite.
     */
    const val EmblemContentDescription: String = "KX"
}

@Composable
fun KXOrb(
    modifier: Modifier = Modifier,
    size: Dp = KXDimens.OrbHero,
    showWordmark: Boolean = true,
    contentDescriptionText: String? = KXOrbTags.EmblemContentDescription,
) {
    val glass = LocalKXGlass.current
    val motion = LocalKXMotion.current

    val orbitProgress: Float
    val haloPulse: Float

    if (motion.reduced) {
        // Static fallback: the artwork is complete, nothing loops.
        orbitProgress = 0f
        haloPulse = 0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "kx-orb")
        orbitProgress = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(motion.orbitPeriodMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "orbit",
        ).value
        haloPulse = transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(motion.haloPeriodMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "halo",
        ).value
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                // Announced once, at the container, so the emblem is a single
                // screen-reader object rather than eleven canvas fragments.
                if (contentDescriptionText != null) {
                    Modifier.semantics { contentDescription = contentDescriptionText }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = minOf(this.size.width, this.size.height)
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = side * OrbRadiusRatio
            val center = Offset(cx, cy - side * OrbVerticalBias)

            drawOuterHalo(center, radius, glass, haloPulse)
            drawOrbBody(center, radius, side, glass)
            clipPath(circlePath(center, radius)) {
                drawFlowingRibbons(center, radius, glass)
                drawOrbitRing(center, radius, side, glass)
                drawOrbitNodes(center, radius, glass, orbitProgress)
                drawSparkles(center, radius, glass, haloPulse)
                drawShield(center, radius, side, glass)
            }
            drawOrbRim(center, radius, side, glass)
            if (showWordmark) drawWordmarkGlow(center, radius)
        }

        if (showWordmark) {
            // Real text, not painted glyphs: it follows the user's font scale,
            // mirrors correctly in RTL and is readable by a screen reader.
            // The Dp footprint is converted to sp so the glyph size still tracks the
            // orb dimensions while honouring the user's font multiplier.
            Text(
                text = "KX",
                style = KXTypography.Wordmark.copy(
                    fontSize = (size * 0.155f).value.sp,
                    lineHeight = (size * 0.17f).value.sp,
                    brush = glass.wordmarkBrush,
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = size * 0.215f),
            )
        }
    }
}

// ---------------------------------------------------------------------------------
// Normalised geometry, measured off the reference artwork
// ---------------------------------------------------------------------------------

private const val OrbRadiusRatio = 0.440f
private const val OrbVerticalBias = 0.007f
private const val ShieldHalfWidthRatio = 0.40f
private const val ShieldTopRatio = -0.72f
private const val ShieldBottomRatio = 0.16f
private const val RingTiltDegrees = -14f
private const val RingCenterBias = -0.06f

// ---------------------------------------------------------------------------------
// Layers
// ---------------------------------------------------------------------------------

private fun DrawScope.drawOuterHalo(
    center: Offset,
    radius: Float,
    glass: KXGlassScheme,
    pulse: Float,
) {
    val haloRadius = radius * 1.34f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to glass.accent.copy(alpha = 0.30f * pulse),
                0.62f to KXPalette.Orchid.copy(alpha = 0.16f * pulse),
                1f to Color.Transparent,
            ),
            center = center,
            radius = haloRadius,
        ),
        radius = haloRadius,
        center = center,
    )
}

private fun DrawScope.drawOrbBody(
    center: Offset,
    radius: Float,
    side: Float,
    glass: KXGlassScheme,
) {
    // Off-centre core reproduces the top-lit pearl in the reference.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to glass.orbCore,
                0.45f to glass.orbMid,
                0.82f to glass.orbMid.copy(alpha = 0.92f),
                1f to glass.orbEdge.copy(alpha = 0.85f),
            ),
            center = Offset(center.x, center.y - radius * 0.28f),
            radius = radius * 1.35f,
        ),
        radius = radius,
        center = center,
    )

    rotate(degrees = -18f, pivot = center) {
        drawPath(
            path = ellipticalArcPath(center, radius * 0.86f, radius * 0.30f, top = true),
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.04f)),
                start = Offset(center.x - radius, center.y - radius),
                end = Offset(center.x + radius, center.y),
            ),
            style = Stroke(width = side * 0.004f),
        )
    }
}

private fun DrawScope.drawFlowingRibbons(
    center: Offset,
    radius: Float,
    glass: KXGlassScheme,
) {
    val bandY = center.y + radius * 0.42f
    val band = Path().apply {
        moveTo(center.x - radius * 1.05f, bandY + radius * 0.30f)
        cubicTo(
            center.x - radius * 0.35f, bandY - radius * 0.34f,
            center.x + radius * 0.30f, bandY + radius * 0.12f,
            center.x + radius * 1.05f, bandY - radius * 0.22f,
        )
        lineTo(center.x + radius * 1.05f, bandY + radius * 0.16f)
        cubicTo(
            center.x + radius * 0.30f, bandY + radius * 0.62f,
            center.x - radius * 0.35f, bandY + radius * 0.16f,
            center.x - radius * 1.05f, bandY + radius * 0.62f,
        )
        close()
    }
    drawPath(
        path = band,
        brush = Brush.linearGradient(
            listOf(
                KXPalette.RibbonBlue.copy(alpha = 0.55f),
                glass.orbCore.copy(alpha = 0.75f),
                KXPalette.Orchid.copy(alpha = 0.40f),
            ),
            start = Offset(center.x - radius, bandY),
            end = Offset(center.x + radius, bandY + radius * 0.2f),
        ),
    )

    val lowerBand = Path().apply {
        val y = center.y + radius * 0.72f
        moveTo(center.x - radius * 1.05f, y)
        cubicTo(
            center.x - radius * 0.2f, y - radius * 0.22f,
            center.x + radius * 0.2f, y + radius * 0.20f,
            center.x + radius * 1.05f, y - radius * 0.06f,
        )
        lineTo(center.x + radius * 1.05f, y + radius * 0.14f)
        cubicTo(
            center.x + radius * 0.2f, y + radius * 0.40f,
            center.x - radius * 0.2f, y - radius * 0.02f,
            center.x - radius * 1.05f, y + radius * 0.20f,
        )
        close()
    }
    drawPath(path = lowerBand, color = KXPalette.RibbonBlue.copy(alpha = 0.35f))
}

private fun DrawScope.drawOrbitRing(
    center: Offset,
    radius: Float,
    side: Float,
    glass: KXGlassScheme,
) {
    val ringCenter = Offset(center.x, center.y + radius * RingCenterBias)
    rotate(degrees = RingTiltDegrees, pivot = ringCenter) {
        drawPath(
            path = ellipticalArcPath(
                center = ringCenter,
                rx = radius * 0.80f,
                ry = radius * 0.30f,
                top = false,
            ),
            color = Color.White.copy(alpha = 0.42f),
            style = Stroke(width = side * 0.0022f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawOrbitNodes(
    center: Offset,
    radius: Float,
    glass: KXGlassScheme,
    orbitProgress: Float,
) {
    // Phase offsets place the nodes where the reference puts them: upper-left blue,
    // lower-left orchid, right pearl.
    val phases = floatArrayOf(0.20f, 0.62f, 0.94f)
    val nodeRadii = floatArrayOf(0.052f, 0.046f, 0.058f)
    val ringCenter = Offset(center.x, center.y + radius * RingCenterBias)
    val rx = radius * 0.80f
    val ry = radius * 0.30f
    val tiltRad = Math.toRadians(RingTiltDegrees.toDouble())

    phases.forEachIndexed { index, phase ->
        val angle = Math.toRadians(((phase + orbitProgress) * 360f).toDouble())
        val localX = rx * cos(angle).toFloat()
        val localY = ry * sin(angle).toFloat()
        val px = ringCenter.x + localX * cos(tiltRad).toFloat() - localY * sin(tiltRad).toFloat()
        val py = ringCenter.y + localX * sin(tiltRad).toFloat() + localY * cos(tiltRad).toFloat()
        val nodeRadius = radius * nodeRadii[index]
        val color = glass.nodeColors.getOrElse(index) { KXPalette.NodeBlue }

        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.95f),
                    0.35f to color,
                    1f to color.copy(alpha = 0.85f),
                ),
                center = Offset(px - nodeRadius * 0.3f, py - nodeRadius * 0.3f),
                radius = nodeRadius * 1.5f,
            ),
            radius = nodeRadius,
            center = Offset(px, py),
        )
    }
}

private fun DrawScope.drawSparkles(
    center: Offset,
    radius: Float,
    glass: KXGlassScheme,
    pulse: Float,
) {
    val points = listOf(
        Offset(center.x + radius * 0.52f, center.y - radius * 0.62f) to radius * 0.075f,
        Offset(center.x - radius * 0.60f, center.y - radius * 0.06f) to radius * 0.055f,
        Offset(center.x - radius * 0.18f, center.y + radius * 0.86f) to radius * 0.045f,
    )
    points.forEach { (point, arm) ->
        drawPath(
            path = fourPointStar(point, arm, arm * 0.26f),
            color = Color.White.copy(alpha = 0.55f + 0.35f * pulse),
        )
    }
}

private fun DrawScope.drawShield(
    center: Offset,
    radius: Float,
    side: Float,
    glass: KXGlassScheme,
) {
    val halfWidth = radius * ShieldHalfWidthRatio
    val top = center.y + radius * ShieldTopRatio
    val bottom = center.y + radius * ShieldBottomRatio
    val shield = crestPath(center, halfWidth, top, bottom)

    clipPath(shield) {
        drawPath(
            path = shield,
            brush = Brush.linearGradient(
                listOf(KXPalette.ShieldViolet, KXPalette.ShieldDeep),
                start = Offset(center.x - halfWidth, top),
                end = Offset(center.x + halfWidth, bottom),
            ),
        )

        // The folded ribbon that forms the X of the mark.
        val bandWidth = halfWidth * 0.62f
        drawPath(
            path = ribbonPath(center, halfWidth, top, bottom, leftToRight = true),
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.92f),
                    KXPalette.Cyan.copy(alpha = 0.85f),
                    KXPalette.RibbonViolet.copy(alpha = 0.75f),
                ),
                start = Offset(center.x - halfWidth, top),
                end = Offset(center.x + halfWidth, bottom),
            ),
            style = Stroke(width = bandWidth, cap = StrokeCap.Round),
        )
        drawPath(
            path = ribbonPath(center, halfWidth, top, bottom, leftToRight = false),
            brush = Brush.linearGradient(
                listOf(
                    KXPalette.Orchid.copy(alpha = 0.80f),
                    Color.White.copy(alpha = 0.70f),
                    KXPalette.NodeBlue.copy(alpha = 0.80f),
                ),
                start = Offset(center.x + halfWidth, top),
                end = Offset(center.x - halfWidth, bottom),
            ),
            style = Stroke(width = bandWidth * 0.92f, cap = StrokeCap.Round),
        )
    }

    drawPath(
        path = shield,
        brush = glass.rimBrush,
        style = Stroke(width = side * 0.004f),
    )
}

private fun DrawScope.drawWordmarkGlow(center: Offset, radius: Float) {
    val glowCenter = Offset(center.x, center.y + radius * 0.60f)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.45f),
                1f to Color.Transparent,
            ),
            center = glowCenter,
            radius = radius * 0.62f,
        ),
        radius = radius * 0.62f,
        center = glowCenter,
    )
}

private fun DrawScope.drawOrbRim(
    center: Offset,
    radius: Float,
    side: Float,
    glass: KXGlassScheme,
) {
    drawCircle(
        brush = glass.rimBrush,
        radius = radius,
        center = center,
        style = Stroke(width = side * RimStrokeRatio),
    )
    // Inner diffraction line just inside the rim.
    drawCircle(
        color = Color.White.copy(alpha = 0.22f),
        radius = radius * 0.955f,
        center = center,
        style = Stroke(width = side * 0.0016f),
    )
}

private const val RimStrokeRatio = 0.0045f

// ---------------------------------------------------------------------------------
// Path helpers (pure; covered by KXOrbGeometryTest)
// ---------------------------------------------------------------------------------

internal fun circlePath(center: Offset, radius: Float): Path = Path().apply {
    addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
}

/** Half-ellipse used for the glass sheen and the orbit ring. */
internal fun ellipticalArcPath(center: Offset, rx: Float, ry: Float, top: Boolean): Path =
    Path().apply {
        val rect = Rect(center.x - rx, center.y - ry, center.x + rx, center.y + ry)
        // 180 degrees of an ellipse; `top` picks the upper or lower half.
        addArc(rect, if (top) 180f else 0f, 180f)
        lineTo(rect.left, if (top) rect.top else rect.bottom)
    }

/** Classic crest: rounded shoulders converging to a point at the bottom. */
internal fun crestPath(center: Offset, halfWidth: Float, top: Float, bottom: Float): Path {
    val cx = center.x
    val left = cx - halfWidth
    val right = cx + halfWidth
    val shoulder = top + halfWidth * 0.34f
    return Path().apply {
        moveTo(cx, top)
        cubicTo(
            right - halfWidth * 0.12f, top - halfWidth * 0.10f,
            right, top + halfWidth * 0.06f,
            right, shoulder,
        )
        cubicTo(
            right, bottom - halfWidth * 0.62f,
            cx + halfWidth * 0.46f, bottom - halfWidth * 0.22f,
            cx, bottom,
        )
        cubicTo(
            cx - halfWidth * 0.46f, bottom - halfWidth * 0.22f,
            left, bottom - halfWidth * 0.62f,
            left, shoulder,
        )
        cubicTo(
            left, top + halfWidth * 0.06f,
            left + halfWidth * 0.12f, top - halfWidth * 0.10f,
            cx, top,
        )
        close()
    }
}

/** One diagonal fold of the ribbon X, bowed so it reads as fabric. */
internal fun ribbonPath(
    center: Offset,
    halfWidth: Float,
    top: Float,
    bottom: Float,
    leftToRight: Boolean,
): Path {
    val cx = center.x
    val x0 = if (leftToRight) cx - halfWidth * 0.92f else cx + halfWidth * 0.92f
    val x1 = if (leftToRight) cx + halfWidth * 0.92f else cx - halfWidth * 0.92f
    val bend = if (leftToRight) halfWidth * 0.30f else -halfWidth * 0.30f
    val midY0 = top + (bottom - top) * 0.30f
    val midY1 = top + (bottom - top) * 0.58f
    return Path().apply {
        moveTo(x0, top + halfWidth * 0.18f)
        cubicTo(cx + bend, midY0, cx + bend, midY1, x1, bottom - halfWidth * 0.10f)
    }
}

/** Four-point twinkle, the shape used by the reference sparkles. */
internal fun fourPointStar(center: Offset, outer: Float, inner: Float): Path {
    val points = listOf(
        Offset(center.x, center.y - outer),
        Offset(center.x + inner, center.y - inner),
        Offset(center.x + outer, center.y),
        Offset(center.x + inner, center.y + inner),
        Offset(center.x, center.y + outer),
        Offset(center.x - inner, center.y + inner),
        Offset(center.x - outer, center.y),
        Offset(center.x - inner, center.y - inner),
    )
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
}
