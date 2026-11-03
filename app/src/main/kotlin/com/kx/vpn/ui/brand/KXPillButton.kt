package com.kx.vpn.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * The full-width glass pill from `01-welcome.png`, reused by any primary action that
 * needs the reference's horizontal control rather than a compact button.
 *
 * The shape is a true capsule (`percent = 50`) and the height is coerced to at least
 * [KXDimens.MinTouchTarget], so the visible pill and the clickable region are the same
 * thing — a rounded rectangle with a large radius would look like a pill but leave the
 * corners dead.
 */
@Composable
fun KXPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    emphasized: Boolean = true,
    enabled: Boolean = true,
    disabledReason: String? = null,
    height: Dp = 60.dp,
) {
    val glass = LocalKXGlass.current
    val shape = RoundedCornerShape(percent = 50)
    val interactionSource = remember { MutableInteractionSource() }
    val effectiveHeight = if (height < KXDimens.MinTouchTarget) {
        KXDimens.MinTouchTarget
    } else {
        height
    }

    val fill = when {
        !enabled -> Brush.linearGradient(
            listOf(glass.glassTop.copy(alpha = 0.30f), glass.glassBottom.copy(alpha = 0.30f)),
        )

        emphasized -> Brush.linearGradient(
            listOf(
                KXPalette.Violet.copy(alpha = 0.92f),
                KXPalette.Orchid.copy(alpha = 0.80f),
                KXPalette.Cyan.copy(alpha = 0.85f),
            ),
        )

        else -> glass.glassFillBrush
    }

    val foreground = when {
        !enabled -> glass.textSecondary.copy(alpha = 0.65f)
        emphasized -> KXPalette.NightTextPrimary
        else -> glass.textPrimary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(effectiveHeight)
                .clip(shape)
                .background(fill, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) {
                    this.contentDescription = label
                    if (!enabled) {
                        disabled()
                        disabledReason?.takeIf { it.isNotBlank() }?.let { reason ->
                            this.contentDescription = "$label. $reason"
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceMd),
                modifier = Modifier.padding(horizontal = KXDimens.SpaceLg),
            ) {
                leadingIcon?.let { icon ->
                    Icon(
                        painter = icon,
                        contentDescription = null, // the merged label already names it
                        tint = foreground,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = label,
                    style = KXTypography.Button.copy(color = foreground, fontSize = 18.sp),
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (!enabled && !disabledReason.isNullOrBlank()) {
            Text(
                text = disabledReason,
                style = KXTypography.Caption.copy(color = glass.textSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KXDimens.SpaceSm),
            )
        }
    }
}
