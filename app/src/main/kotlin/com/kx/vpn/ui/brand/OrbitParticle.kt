package com.kx.vpn.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette

/**
 * A single jewel node — the same sphere family the three orbiting nodes of
 * [KXOrb] are drawn with, exposed so background decoration in later screens
 * reuses one implementation instead of a second copy of the gradient maths.
 *
 * Decorative by default: it announces itself to accessibility services only when
 * the caller supplies a description, because ambient particles must not become
 * screen-reader noise.
 */
@Composable
fun OrbitParticle(
    modifier: Modifier = Modifier,
    diameter: Dp = KXDimens.SpaceSm,
    tint: Color = KXPalette.NodeBlue,
    alpha: Float = 0.85f,
    contentDescriptionText: String? = null,
) {
    Canvas(
        modifier = modifier
            .size(diameter)
            .then(
                if (contentDescriptionText != null) {
                    Modifier.semantics { contentDescription = contentDescriptionText }
                } else {
                    Modifier
                },
            ),
    ) {
        val r = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.White.copy(alpha = 0.95f * alpha),
                    0.35f to tint.copy(alpha = alpha),
                    1f to tint.copy(alpha = alpha * 0.7f),
                ),
                center = Offset(center.x - r * 0.3f, center.y - r * 0.3f),
                radius = r * 1.5f,
            ),
            radius = r,
            center = center,
        )
    }
}
