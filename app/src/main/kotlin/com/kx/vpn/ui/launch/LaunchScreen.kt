package com.kx.vpn.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kx.vpn.R
import com.kx.vpn.domain.LaunchDestination
import com.kx.vpn.ui.brand.KXOrb
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass
import com.kx.vpn.ui.theme.LocalKXMotion

/** Stable automation hook for the explicit human-entry action. */
object KXEntryTags {
    const val EnterOrb = "kx_enter_orb"
}

/**
 * The branded launch experience, shown after the system splash and before the first
 * real destination.
 *
 * There is no progress percentage anywhere on this screen. The only thing it claims
 * is the identity, plus a truthful "preparing" line while the persisted launch state
 * is still being read.
 */
@Composable
fun LaunchScreen(
    viewModel: LaunchViewModel,
    onNavigate: (LaunchDestination, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val glass = LocalKXGlass.current
    val motion = LocalKXMotion.current

    // Real intro: the emblem settles into place. Under reduced motion the duration is
    // zero, so nothing moves and nothing loops.
    val intro = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(motion.reduced) {
        intro.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = motion.introMillis,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    // Never auto-enter: readiness unlocks the emblem, but only a real tap navigates.
    // Track clicks locally so a rapid double tap cannot enqueue two destinations.
    val resolved = state as? LaunchUiState.Resolved
    var entered by remember { mutableStateOf(false) }
    val canEnter = resolved != null && intro.value >= 1f && !entered

    val shortestSide = LocalConfiguration.current.screenWidthDp.dp
    val orbSize = when {
        shortestSide < 360.dp -> KXDimens.OrbLarge
        else -> KXDimens.OrbHero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(glass.background, glass.surface, glass.background),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = KXDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            KXOrb(
                size = orbSize,
                modifier = Modifier
                    // Native 3D perspective, without replacing the source artwork.
                    // The intro settles before the user can enter.
                    .graphicsLayer {
                        rotationY = -20f * (1f - intro.value)
                        rotationX = 12f * (1f - intro.value)
                        cameraDistance = 1600f * density
                        shadowElevation = (12f * density) * intro.value
                    }
                    .scale(0.84f + 0.16f * intro.value)
                    .alpha(intro.value)
                    .testTag(KXEntryTags.EnterOrb)
                    .clickable(
                        enabled = canEnter,
                        role = Role.Button,
                        onClick = {
                            val ready = resolved
                            if (!entered && ready != null) {
                                entered = true
                                onNavigate(ready.destination, ready.storageDegraded)
                            }
                        },
                    ),
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceXl))

            Text(
                text = stringResource(R.string.launch_tagline),
                style = KXTypography.Title.copy(color = glass.textPrimary),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceSm))

            Text(
                text = stringResource(
                    if (canEnter) R.string.launch_enter_hint else R.string.launch_preparing,
                ),
                style = KXTypography.Caption.copy(color = glass.textSecondary),
                textAlign = TextAlign.Center,
            )
        }

        // Footnote states the security truth without making claims about live tunnel state.
        Text(
            text = stringResource(R.string.launch_no_tunnel_notice),
            style = KXTypography.Caption.copy(color = KXPalette.NeutralHint),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(horizontal = KXDimens.ScreenPadding)
                .padding(bottom = KXDimens.SpaceMd, top = KXDimens.SpaceMd),
        )
    }
}
