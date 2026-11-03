package com.kx.vpn.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kx.vpn.R
import com.kx.vpn.ui.brand.KXAtmosphericBackground
import com.kx.vpn.ui.brand.KXOrb
import com.kx.vpn.ui.brand.KXPillButton
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * First-run welcome screen, the native implementation of
 * `assets/reference/01-welcome.png`.
 *
 * Both controls are real and both go somewhere real. The reference's tagline
 * "Private. Fast. Borderless." is deliberately not reproduced: the task contract bans
 * unsupported private/borderless absolutes, and this build has no tunnel engine at all,
 * so the honest line replaces it and the standing notice stays on screen.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onImportConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current
    val shortestSide = LocalConfiguration.current.screenWidthDp.dp

    // The reference hero fills most of a 941x1672 canvas. Below 360dp that would push
    // the controls off-screen, so the emblem gives up size before the buttons do.
    val orbSize = when {
        shortestSide < 340.dp -> KXDimens.OrbLarge - 40.dp
        shortestSide < 360.dp -> KXDimens.OrbLarge
        else -> KXDimens.OrbHero + 24.dp
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
            Spacer(modifier = Modifier.height(KXDimens.SpaceXl))

            KXOrb(size = orbSize)

            Spacer(modifier = Modifier.height(KXDimens.SpaceXl))

            WordmarkBlock(textColor = glass.textPrimary, accent = glass.accent)

            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

            Text(
                text = stringResource(R.string.welcome_tagline),
                style = KXTypography.Body.copy(
                    color = glass.textPrimary,
                    fontStyle = FontStyle.Italic,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceXxl))

            KXPillButton(
                label = stringResource(R.string.welcome_get_started),
                onClick = onGetStarted,
                leadingIcon = painterResource(R.drawable.ic_kx_arrow_forward),
                emphasized = true,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

            KXPillButton(
                label = stringResource(R.string.welcome_import_config),
                onClick = onImportConfig,
                leadingIcon = painterResource(R.drawable.ic_kx_cloud_upload),
                emphasized = false,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

            Text(
                text = stringResource(R.string.welcome_honesty_notice),
                style = KXTypography.Caption.copy(color = KXPalette.NeutralHint),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceXl))
        }
    }
}

/**
 * "KX" over "Private VPN" with the thin luminous rules either side, as in the
 * reference. The rules are drawn, not characters, and they sit on both sides so the
 * block stays symmetric when the layout direction flips to RTL.
 */
@Composable
private fun WordmarkBlock(textColor: Color, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.welcome_wordmark),
            style = KXTypography.Display.copy(
                color = textColor,
                fontSize = 52.sp,
                lineHeight = 58.sp,
                letterSpacing = 2.sp,
            ),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AccentRule(Modifier.weight(1f), accent)
            Spacer(modifier = Modifier.width(KXDimens.SpaceMd))
            Text(
                text = stringResource(R.string.welcome_wordmark_sub),
                style = KXTypography.Title.copy(
                    color = textColor.copy(alpha = 0.92f),
                    letterSpacing = 5.sp,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(KXDimens.SpaceMd))
            AccentRule(Modifier.weight(1f), accent)
        }
    }
}

@Composable
private fun AccentRule(modifier: Modifier, accent: Color) {
    Box(
        modifier = modifier
            .height(1.dp)
            // Rules fade inward-to-outward; reversed on the trailing side would need a
            // second brush, so both use the same symmetric fade.
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.75f), Color.Transparent),
                ),
            ),
    )
}
