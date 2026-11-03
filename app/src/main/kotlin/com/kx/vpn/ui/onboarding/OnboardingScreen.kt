package com.kx.vpn.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kx.vpn.R
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.KXAtmosphericBackground
import com.kx.vpn.ui.brand.KXPillButton
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * The disclosure step between Welcome and Home.
 *
 * Its job is to say what a VPN does *not* do. `docs/tasks/TASK_01` requires transparent
 * information about what a VPN is, its privacy limits and its cost boundaries, and bans
 * unsupported absolutes — so the copy here deliberately avoids "anonymous", "unlimited",
 * "no logs" and "borderless", and Continue stays disabled until the disclosure has been
 * acknowledged rather than being pre-ticked.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val glass = LocalKXGlass.current

    // Navigation is a consequence of a successful write, never of the tap itself.
    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }

    KXAtmosphericBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KXDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

            Text(
                text = stringResource(R.string.onboarding_title),
                style = KXTypography.Display.copy(color = glass.textPrimary),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceSm))

            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = KXTypography.Body.copy(color = glass.textSecondary),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

            DisclosureCard(
                title = stringResource(R.string.onboarding_section_what),
                body = stringResource(R.string.onboarding_body_what),
            )
            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))
            DisclosureCard(
                title = stringResource(R.string.onboarding_section_limits),
                body = stringResource(R.string.onboarding_body_limits),
            )
            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))
            DisclosureCard(
                title = stringResource(R.string.onboarding_section_cost),
                body = stringResource(R.string.onboarding_body_cost),
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceSm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Checkbox(
                    checked = state.disclosureAcknowledged,
                    onCheckedChange = { checked ->
                        if (checked) viewModel.acknowledgeDisclosure()
                    },
                    modifier = Modifier.size(KXDimens.MinTouchTarget),
                )
                Text(
                    text = stringResource(R.string.onboarding_acknowledge),
                    style = KXTypography.Body.copy(color = glass.textPrimary),
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.failure != null) {
                Text(
                    text = stringResource(R.string.onboarding_write_failed),
                    style = KXTypography.Caption.copy(color = KXPalette.NodeOrchid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = KXDimens.SpaceSm),
                    textAlign = TextAlign.Start,
                )
            }

            Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

            KXPillButton(
                label = stringResource(R.string.onboarding_continue),
                onClick = { viewModel.completeOnboarding() },
                enabled = state.canComplete,
                disabledReason = if (!state.disclosureAcknowledged) {
                    stringResource(R.string.onboarding_continue_locked)
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

            Text(
                text = stringResource(R.string.welcome_honesty_notice),
                style = KXTypography.Caption.copy(color = KXPalette.NeutralHint),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceXl))
        }
    }
}

@Composable
private fun DisclosureCard(title: String, body: String) {
    val glass = LocalKXGlass.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(KXDimens.SpaceXs)) {
            Text(
                text = title,
                style = KXTypography.Caption.copy(color = glass.accent),
            )
            Text(
                text = body,
                style = KXTypography.Body.copy(color = glass.textPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
