package com.kx.vpn.ui.brand

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * The brand glass toggle switch matching the reference designs (05-security-center.png, etc.).
 *
 * Smooth gradient track when active, translucent glass track when inactive, and a white pill/thumb
 * with fluid animation.
 */
@Composable
fun KXSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val glass = LocalKXGlass.current
    val trackWidth = 50.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val thumbPadding = 3.dp

    val maxOffset = trackWidth - thumbSize - (thumbPadding * 2)
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "thumb_offset",
    )

    val trackBrush = if (checked) {
        Brush.horizontalGradient(
            listOf(
                KXPalette.Violet,
                KXPalette.RibbonViolet,
                KXPalette.Cyan,
            ),
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                glass.glassTop.copy(alpha = 0.4f),
                glass.glassBottom.copy(alpha = 0.4f),
            ),
        )
    }

    val borderColor = if (checked) {
        KXPalette.OrbRimCyan.copy(alpha = 0.6f)
    } else {
        glass.glassBorder.copy(alpha = 0.5f)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(4.dp)
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackBrush)
            .border(1.dp, borderColor, RoundedCornerShape(percent = 50))
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Switch,
                        onClick = { onCheckedChange(!checked) },
                    )
                } else Modifier
            )
            .semantics {
                role = Role.Switch
                stateDescription = if (checked) "On" else "Off"
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbPadding)
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(0.5.dp, Color(0x33000000), CircleShape),
        )
    }
}
