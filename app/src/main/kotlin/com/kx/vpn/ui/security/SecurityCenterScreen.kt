package com.kx.vpn.ui.security

import android.content.Intent
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kx.vpn.R
import com.kx.vpn.data.SecuritySettings
import com.kx.vpn.domain.security.SecurityControlKey
import com.kx.vpn.domain.security.SecurityEnforcement
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.GlassCircle
import com.kx.vpn.ui.brand.KXAtmosphericBackground
import com.kx.vpn.ui.brand.KXInteriorHeader
import com.kx.vpn.ui.brand.KXOrb
import com.kx.vpn.ui.brand.KXSwitch
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * Security Center Screen - assets/reference/05-security-center.png
 *
 * Displays the current protection status (strictly derived from the real tunnel state:
 * "Fully Protected" only when tunnel is actually up) and 6 persistence-backed security
 * controls.
 */
@Composable
fun SecurityCenterScreen(
    isTunnelUp: Boolean,
    settings: SecuritySettings,
    onToggleKillSwitch: (Boolean) -> Unit,
    onToggleDnsLeakProtection: (Boolean) -> Unit,
    onToggleIpv6Protection: (Boolean) -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit,
    onToggleTrackerBlocking: (Boolean) -> Unit,
    onToggleAlwaysOnVpn: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current
    val context = LocalContext.current

    KXAtmosphericBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KXInteriorHeader(onBack = onBack)

            Spacer(modifier = Modifier.height(KXDimens.SpaceXs))

            Text(
                text = stringResource(R.string.security_center_title),
                style = KXTypography.Title.copy(
                    color = glass.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

            // Large Hero Card with Orb and Protection Status
            SecurityHeroCard(isTunnelUp = isTunnelUp)

            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

            // 6 Security Toggle Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KXDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SecurityToggleRow(
                    iconRes = R.drawable.ic_kx_power,
                    titleRes = R.string.security_kill_switch_title,
                    descRes = R.string.security_kill_switch_desc,
                    key = SecurityControlKey.KILL_SWITCH,
                    note = stringResource(R.string.security_note_kill_switch),
                    checked = settings.killSwitch,
                    onCheckedChange = onToggleKillSwitch,
                )

                SecurityToggleRow(
                    iconRes = R.drawable.ic_kx_dns,
                    titleRes = R.string.security_dns_leak_title,
                    descRes = R.string.security_dns_leak_desc,
                    key = SecurityControlKey.DNS_LEAK_PROTECTION,
                    note = stringResource(R.string.security_note_dns_leak, SecurityEnforcement.DNS_LEAK_RESOLVERS),
                    checked = settings.dnsLeakProtection,
                    onCheckedChange = onToggleDnsLeakProtection,
                )

                SecurityToggleRow(
                    iconRes = R.drawable.ic_kx_ipv6,
                    titleRes = R.string.security_ipv6_title,
                    descRes = R.string.security_ipv6_desc,
                    key = SecurityControlKey.IPV6_PROTECTION,
                    note = stringResource(R.string.security_note_ipv6),
                    checked = settings.ipv6Protection,
                    onCheckedChange = onToggleIpv6Protection,
                )

                SecurityToggleRow(
                    iconRes = R.drawable.ic_kx_auto_reconnect,
                    titleRes = R.string.security_auto_reconnect_title,
                    descRes = R.string.security_auto_reconnect_desc,
                    key = SecurityControlKey.AUTO_RECONNECT,
                    note = stringResource(R.string.security_note_auto_reconnect),
                    checked = settings.autoReconnect,
                    onCheckedChange = onToggleAutoReconnect,
                )

                SecurityToggleRow(
                    iconRes = R.drawable.ic_kx_tracker,
                    titleRes = R.string.security_tracker_blocking_title,
                    descRes = R.string.security_tracker_blocking_desc,
                    key = SecurityControlKey.TRACKER_BLOCKING,
                    note = stringResource(R.string.security_note_tracker, SecurityEnforcement.TRACKER_RESOLVERS),
                    checked = settings.trackerBlocking,
                    onCheckedChange = onToggleTrackerBlocking,
                )

                SecurityToggleRow(
                    iconRes = R.drawable.ic_kx_infinity,
                    titleRes = R.string.security_always_on_title,
                    descRes = R.string.security_always_on_desc,
                    key = SecurityControlKey.ALWAYS_ON_VPN,
                    note = stringResource(R.string.security_note_always_on),
                    checked = settings.alwaysOnVpn,
                    onCheckedChange = { checked ->
                        onToggleAlwaysOnVpn(checked)
                        // Per Rule 6: Always-On VPN navigates to Android Settings
                        runCatching {
                            val intent = Intent(Settings.ACTION_VPN_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(KXDimens.SpaceXl))
        }
    }
}

@Composable
private fun SecurityHeroCard(
    isTunnelUp: Boolean,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current
    // The reference artwork says "Fully Protected / Your connection and data are secured".
    // This build cannot support that sentence: what it knows is that the backend reported
    // the tunnel interface up. Whether traffic actually leaves through the tunnel, and
    // whether anything leaks around it, has not been verified — docs/SECURITY_MODEL.md
    // requires "VPN UP" and "Internet Verified" to stay distinct, so the card states the
    // first and says plainly that the second is missing.
    val statusText = if (isTunnelUp) {
        stringResource(R.string.security_status_tunnel_up)
    } else {
        stringResource(R.string.security_status_not_connected)
    }
    val subtitleText = if (isTunnelUp) {
        stringResource(R.string.security_subtitle_not_verified)
    } else {
        stringResource(R.string.security_subtitle_down)
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KXDimens.ScreenPadding),
        cornerRadius = 24.dp,
        contentPadding = 20.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KXOrb(
                size = 140.dp,
                showWordmark = false,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTunnelUp) Color(0xFF10B981) else KXPalette.NeutralHint,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_kx_check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = statusText,
                    style = KXTypography.Body.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = glass.textPrimary,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitleText,
                style = KXTypography.Caption.copy(
                    color = glass.textSecondary,
                    fontSize = 13.sp,
                ),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SecurityToggleRow(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes descRes: Int,
    key: SecurityControlKey,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    note: String? = null,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current
    val title = stringResource(titleRes)
    val desc = stringResource(descRes)
    // A control the build does not honour is not merely labelled: it is not operable.
    // Tapping a switch that saves a value nobody reads teaches the user that security
    // settings are decoration, which is worse than admitting the feature is not built.
    // The table in the domain is the only place that decides this; the screen
    // asks it rather than repeating an answer that could drift out of date.
    val enforcement = SecurityEnforcement.of(key)
    val operable = SecurityEnforcement.isOperable(key)
    val badgeText = when {
        !operable -> stringResource(R.string.security_badge_not_enforced)
        checked -> stringResource(R.string.security_badge_on)
        else -> stringResource(R.string.security_badge_off)
    }
    val spoken = buildString {
        append(title); append(". ")
        append(badgeText); append(". ")
        append(desc)
        if (note != null) { append(". "); append(note) }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = spoken
            },
        cornerRadius = 18.dp,
        contentPadding = 12.dp,
        highlight = false,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon in glass circle
            GlassCircle(diameter = 44.dp) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = KXPalette.Cyan,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = KXTypography.Body.copy(
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary,
                            fontSize = 15.sp,
                        ),
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Small Badge: "✓ On"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(
                                // Green means "this is happening". A control that is not
                                // honoured never gets it, whatever the stored value says.
                                when {
                                    !operable -> Color(0x1A000000)
                                    checked -> Color(0x2610B981)
                                    else -> Color(0x1A000000)
                                },
                                RoundedCornerShape(percent = 50),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (operable && checked) "✓ $badgeText" else badgeText,
                            style = KXTypography.Caption.copy(
                                color = if (operable && checked) {
                                    Color(0xFF10B981)
                                } else {
                                    glass.textSecondary
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = desc,
                    style = KXTypography.Caption.copy(
                        color = glass.textSecondary,
                        fontSize = 12.sp,
                    ),
                    maxLines = 2,
                )

                // The disclosure line is where the truth about the control goes: what it
                // actually changes, or why it cannot be switched yet.
                if (note != null) {
                    Text(
                        text = note,
                        style = KXTypography.Caption.copy(
                            color = if (operable) glass.textSecondary else KXPalette.NeutralHint,
                            fontSize = 11.sp,
                        ),
                        maxLines = 2,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            KXSwitch(
                checked = checked && operable,
                onCheckedChange = if (operable) onCheckedChange else null,
                enabled = operable,
            )
        }
    }
}
