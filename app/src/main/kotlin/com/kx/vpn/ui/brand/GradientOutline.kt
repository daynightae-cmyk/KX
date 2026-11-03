package com.kx.vpn.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette

/**
 * Draws a gradient outline over the current content without adding a layout node.
 *
 * Implemented as a draw pass rather than `Modifier.border` because the brand rim is
 * a *sweep* gradient (`docs/DESIGN_SYSTEM.md`: "gradient rim highlights"), which the
 * stock border modifier cannot express.
 */
fun Modifier.kxGradientOutline(
    strokeWidth: Dp = KXDimens.RimStrokeWidth,
    cornerRadius: Dp = KXDimens.CardCorner,
    brush: Brush = Brush.sweepGradient(
        listOf(
            KXPalette.OrbRimCyan,
            KXPalette.Violet,
            KXPalette.Orchid,
            KXPalette.Cyan,
            KXPalette.OrbRimCyan,
        ),
    ),
    alpha: Float = 1f,
): Modifier = this.drawWithContent {
    drawContent()
    val inset = strokeWidth.toPx() / 2f
    drawRoundRect(
        brush = brush,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        alpha = alpha,
        style = Stroke(width = strokeWidth.toPx()),
    )
}

/**
 * Circular gradient rim used around the emblem. Kept as a composable so previews can
 * show the rim on its own when the orb artwork is being reviewed.
 */
@Composable
fun GradientOutlineRing(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = KXDimens.RimStrokeWidth,
) {
    val stops = listOf(
        KXPalette.OrbRimCyan,
        KXPalette.Violet,
        KXPalette.Orchid,
        KXPalette.Cyan,
        KXPalette.OrbRimCyan,
    )
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f - strokeWidth.toPx() / 2f
            drawCircle(
                brush = Brush.sweepGradient(stops),
                radius = r,
                center = Offset(this.size.width / 2f, this.size.height / 2f),
                style = Stroke(width = strokeWidth.toPx()),
            )
        }
    }
}
