package com.kx.vpn.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

enum class KXButtonStyle { Primary, Secondary, Ghost }

/**
 * The brand button.
 *
 * `disabledReason` is not decoration: the quality gate requires that anything a
 * user cannot use yet is "explicitly disabled with reason". A caller whose feature
 * is waiting on an external dependency passes the reason here, and the control
 * renders the explanation instead of silently doing nothing.
 *
 * `fillWidth` stretches the button itself, not just the wrapper. Without it a caller
 * passing `Modifier.fillMaxWidth()` gets a full-width column with a content-sized
 * button sitting at its start, which is what the connected screen's Disconnect control
 * did before this parameter existed.
 */
@Composable
fun KXButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    style: KXButtonStyle = KXButtonStyle.Primary,
    enabled: Boolean = true,
    busy: Boolean = false,
    disabledReason: String? = null,
    minWidth: Dp = 120.dp,
) {
    val glass = LocalKXGlass.current
    val shape = RoundedCornerShape(KXDimens.ButtonCorner)
    val interactionSource = remember { MutableInteractionSource() }

    val effectiveEnabled = enabled && !busy

    val fill: Brush? = when {
        !effectiveEnabled -> Brush.linearGradient(
            listOf(glass.glassTop.copy(alpha = 0.35f), glass.glassBottom.copy(alpha = 0.35f)),
        )

        style == KXButtonStyle.Primary -> Brush.linearGradient(
            listOf(KXPalette.Violet, KXPalette.Orchid, KXPalette.Cyan),
        )

        style == KXButtonStyle.Secondary -> glass.glassFillBrush
        else -> null
    }

    val foreground = when {
        !effectiveEnabled -> glass.textSecondary.copy(alpha = 0.7f)
        style == KXButtonStyle.Primary -> KXPalette.NightTextPrimary
        else -> glass.textPrimary
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = minWidth, minHeight = KXDimens.MinTouchTarget)
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .clip(shape)
                .then(if (fill != null) Modifier.background(fill, shape) else Modifier)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = effectiveEnabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics(mergeDescendants = true) {
                    this.contentDescription = label
                    if (!effectiveEnabled) {
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
                horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceSm),
                modifier = Modifier.padding(horizontal = KXDimens.SpaceLg),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = foreground,
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    text = label,
                    style = KXTypography.Button.copy(color = foreground),
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (!effectiveEnabled && !disabledReason.isNullOrBlank()) {
            Text(
                text = disabledReason,
                style = KXTypography.Caption.copy(color = glass.textSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = KXDimens.SpaceSm),
            )
        }
    }
}
