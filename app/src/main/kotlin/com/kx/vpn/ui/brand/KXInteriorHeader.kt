package com.kx.vpn.ui.brand

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kx.vpn.R
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * Standard interior screen header (used across screens 05-10).
 *
 * Left: circular glass back button (40dp)
 * Center: KXOrb emblem (small shield + wordmark)
 * Right: optional right action (e.g. overflow, settings, or balanced 40dp spacer)
 */
@Composable
fun KXInteriorHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    rightContent: @Composable (() -> Unit)? = null,
) {
    val glass = LocalKXGlass.current
    val backDescription = stringResource(R.string.security_back_content_description)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = KXDimens.ScreenPadding,
                vertical = KXDimens.SpaceSm,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircle(
            diameter = 40.dp,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onBack,
                )
                .semantics {
                    role = Role.Button
                    contentDescription = backDescription
                },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_kx_back),
                contentDescription = null,
                tint = glass.textPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.Center),
            )
        }

        KXOrb(
            size = 38.dp,
            showWordmark = true,
        )

        if (rightContent != null) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                rightContent()
            }
        } else {
            Box(modifier = Modifier.size(40.dp))
        }
    }
}
