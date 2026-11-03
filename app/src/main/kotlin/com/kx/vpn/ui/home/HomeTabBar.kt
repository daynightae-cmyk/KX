package com.kx.vpn.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kx.vpn.R
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

/** The five destinations the reference bottom bar shows. */
enum class HomeTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Home(R.string.home_tab_home, R.drawable.ic_kx_nav_home),
    Servers(R.string.home_tab_servers, R.drawable.ic_kx_nav_servers),
    Security(R.string.home_tab_security, R.drawable.ic_kx_nav_security),
    Stats(R.string.home_tab_stats, R.drawable.ic_kx_nav_stats),
    Settings(R.string.home_tab_settings, R.drawable.ic_kx_nav_settings),
}

/**
 * The bottom navigation bar.
 *
 * Only Home is live in TASK_02. The other four are rendered in their inactive style
 * and are **disabled**, not merely unhandled: a tab that looks tappable but does
 * nothing is the "invisible or unreachable control" the visual QA list forbids, and
 * the disabled state announces why.
 */
@Composable
fun HomeTabBar(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current
    val unavailableLabel = stringResource(R.string.home_tab_coming_soon)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, glass.background.copy(alpha = 0.92f)),
                ),
            )
            .padding(horizontal = KXDimens.SpaceMd, vertical = KXDimens.SpaceSm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceXs),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(percent = 50))
                .background(glass.glassFillBrush, RoundedCornerShape(percent = 50))
                .padding(horizontal = KXDimens.SpaceXs, vertical = KXDimens.SpaceSm),
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val enabled = isSelected
                // Resolved here, in a composable scope: stringResource cannot be called
                // from inside the semantics block below.
                val label = stringResource(tab.labelRes)
                val interactionSource =
                    remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (isSelected) {
                                Brush.linearGradient(
                                    listOf(
                                        glass.accent.copy(alpha = 0.55f),
                                        glass.orbMid.copy(alpha = 0.35f),
                                    ),
                                )
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                            RoundedCornerShape(percent = 50),
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            onClick = { onSelect(tab) },
                        )
                        // clearAndSetSemantics replaces the children's semantics rather
                        // than merging with them, so a tab is announced once instead of
                        // as an icon plus a label plus the tab.
                        .clearAndSetSemantics {
                            contentDescription = if (enabled) label else "$label. $unavailableLabel"
                            role = Role.Button
                            if (!enabled) disabled()
                        }
                        .padding(vertical = KXDimens.SpaceXs),
                ) {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                        tint = if (isSelected) glass.textPrimary else glass.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(tab.labelRes),
                        style = KXTypography.Caption.copy(
                            color = if (isSelected) glass.textPrimary else glass.textSecondary,
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
