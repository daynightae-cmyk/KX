package com.kx.vpn.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * Layered translucent panel with a gradient rim — the surface every later screen
 * stacks its content on.
 *
 * Deliberately uses gradient fills rather than a runtime blur pass:
 * `docs/DESIGN_SYSTEM.md` warns against expensive bitmap blur in continuously
 * scrolling lists, and a blurred card inside a lazy list is exactly where frame
 * budget disappears.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = KXDimens.CardCorner,
    contentPadding: Dp = KXDimens.SpaceLg,
    highlight: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalKXGlass.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(glass.glassFillBrush, shape)
            .border(KXDimens.GlassStrokeWidth, glass.glassBorder, shape)
            .padding(contentPadding),
    ) {
        if (highlight) {
            // Top inner specular line, the "glass edge catching light" cue.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to glass.innerHighlight,
                                0.28f to Color.Transparent,
                                1f to Color.Transparent,
                            ),
                        ),
                        shape = shape,
                    ),
            )
        }
        content()
    }
}

/** Fully circular glass token, used for the inline brand chip in later rows. */
@Composable
fun GlassCircle(
    modifier: Modifier = Modifier,
    diameter: Dp = KXDimens.OrbInline,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val glass = LocalKXGlass.current
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(glass.glassFillBrush, CircleShape)
            .border(KXDimens.GlassStrokeWidth, glass.glassBorder, CircleShape),
        content = content,
    )
}
