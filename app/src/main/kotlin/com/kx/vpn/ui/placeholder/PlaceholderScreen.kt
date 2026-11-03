package com.kx.vpn.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kx.vpn.R
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.KXButton
import com.kx.vpn.ui.brand.KXButtonStyle
import com.kx.vpn.ui.brand.KXOrb
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * The destination the launch gate lands on for TASK_00.
 *
 * It is a real, navigable, scrollable screen built from the shared glass primitives,
 * and it says plainly that the feature behind it does not exist yet. That is the
 * point: TASK_00 must not imply that onboarding, servers or a tunnel are available.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    body: String,
    actionLabel: String,
    actionDisabledReason: String,
    storageDegraded: Boolean,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(glass.background)
            .systemBarsPadding()
            .padding(horizontal = KXDimens.ScreenPadding)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(KXDimens.SpaceXxl))

        KXOrb(size = KXDimens.OrbMedium)

        Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

        Text(
            text = title,
            style = KXTypography.Display.copy(color = glass.textPrimary),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

        Text(
            text = body,
            style = KXTypography.Body.copy(color = glass.textSecondary),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(KXDimens.SpaceXl))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(KXDimens.SpaceMd),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.placeholder_build_section),
                    style = KXTypography.Caption.copy(color = glass.textSecondary),
                )
                Text(
                    text = stringResource(R.string.placeholder_build_detail),
                    style = KXTypography.Body.copy(color = glass.textPrimary),
                )
                KXButton(
                    label = actionLabel,
                    onClick = { /* intentionally inert: the control is disabled */ },
                    style = KXButtonStyle.Secondary,
                    enabled = false,
                    disabledReason = actionDisabledReason,
                    modifier = Modifier.padding(top = KXDimens.SpaceSm),
                )
            }
        }

        if (storageDegraded) {
            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = KXDimens.CardInnerCorner,
            ) {
                Text(
                    text = stringResource(R.string.launch_storage_degraded),
                    style = KXTypography.Body.copy(color = KXPalette.NodeOrchid),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(KXDimens.SpaceXxl))
    }
}
