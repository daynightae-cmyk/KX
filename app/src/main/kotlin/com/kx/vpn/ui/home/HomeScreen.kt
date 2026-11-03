package com.kx.vpn.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kx.vpn.R
import com.kx.vpn.data.TunnelSessionState
import com.kx.vpn.domain.tunnel.FailureReason
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.KXOrb
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass
import com.kx.vpn.ui.theme.LocalKXMotion

/**
 * The home screen, the native implementation of
 * `assets/reference/02-home-disconnected.png`.
 *
 * The reference artwork shows a "SMART CHOICE / Best Location" card and Ping /
 * Download / Upload / IP Masking metrics. None of those values exist in this build —
 * there is no server fleet, no gateway and no measured traffic — so they are rendered
 * as `--` with an explicit "not measured" note rather than as invented numbers, and
 * the location card says plainly that no location has been chosen. Inventing them
 * would be exactly the failure `docs/SECURITY_MODEL.md` names.
 */
@Composable
fun HomeScreen(
    session: TunnelSessionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KXDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

        HomeHeader()

        Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

        ConnectOrb(
            state = session.tunnelState,
            canConnect = session.canConnect,
            canDisconnect = session.canDisconnect,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
        )

        Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

        session.failure?.let { reason ->
            FailureNotice(reason)
            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))
        }

        ChoiceCard()

        Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

        session.elapsedMillis?.let { elapsed ->
            Text(
                text = stringResource(R.string.home_tunnel_duration,
                    com.kx.vpn.domain.tunnel.formatSessionDuration(elapsed)),
                style = KXTypography.Body.copy(color = glass.textPrimary),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))
        }
        MetricGrid(session)

        Spacer(modifier = Modifier.height(KXDimens.SpaceXl))
    }
}

@Composable
private fun HomeHeader() {
    val glass = LocalKXGlass.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        KXOrb(size = 48.dp, showWordmark = false, contentDescriptionText = null)
        Spacer(modifier = Modifier.width(KXDimens.SpaceMd))
        Text(
            text = stringResource(R.string.home_title),
            style = KXTypography.Title.copy(color = glass.textPrimary),
            modifier = Modifier.weight(1f),
        )
        HeaderIcon(R.drawable.ic_kx_bell, stringResource(R.string.home_tab_stats))
        Spacer(modifier = Modifier.width(KXDimens.SpaceXs))
        HeaderIcon(R.drawable.ic_kx_overflow, stringResource(R.string.home_tab_settings))
    }
}

@Composable
private fun HeaderIcon(iconRes: Int, description: String) {
    val glass = LocalKXGlass.current
    // MutableInteractionSource() is remember-backed; constructing it inline in the
    // modifier chain allocates a new one on every recomposition.
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(KXDimens.MinTouchTarget)
            .clip(CircleShape)
            .background(glass.glassFillBrush, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = glass.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * The large connect control.
 *
 * It reflects the real session state: it is a live button only when a profile exists
 * and nothing is running, and the label comes from the state machine rather than from
 * a local flag. While the backend is actually working, a ring pulses — and only then,
 * so the motion is tied to real work rather than decorating a static screen.
 */
@Composable
private fun ConnectOrb(
    state: TunnelState,
    canConnect: Boolean,
    canDisconnect: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val glass = LocalKXGlass.current
    val motion = LocalKXMotion.current

    val busy = state == TunnelState.CONNECTING ||
        state == TunnelState.DISCONNECTING ||
        state == TunnelState.PREPARING_PERMISSION ||
        state == TunnelState.AWAITING_PERMISSION

    // The pulse runs only while the backend is genuinely working. An always-on
    // infinite transition would tick frames forever on an idle screen, which is the
    // "unnecessary battery consumption" the visual QA list warns about, and a
    // conditional remember() is illegal in Compose besides.
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(busy, motion.reduced) {
        if (busy && !motion.reduced) {
            while (true) {
                pulse.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                pulse.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }
        } else {
            pulse.snapTo(0f)
        }
    }

    val connectInteractionSource =
        remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val statusText = stringResource(statusLabel(state))
    val actionLabel = stringResource(
        if (canDisconnect) R.string.home_disconnect else R.string.home_connect,
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(268.dp)
                .clip(CircleShape)
                .background(
                    // The reference orb is a layered glass sphere: a bright outer body,
                    // a darker lens inside it and a luminous edge. A single flat radial
                    // gradient read as a plain button, so the layers are explicit.
                    Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to glass.orbCore.copy(alpha = 0.30f),
                            0.52f to glass.orbMid.copy(alpha = 0.34f),
                            0.86f to glass.orbMid.copy(alpha = 0.20f),
                            1f to Color.Transparent,
                        ),
                    ),
                    CircleShape,
                )
                .drawBehind {
                    // Luminous rim.
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                glass.accent.copy(alpha = 0.85f),
                                glass.orbCore.copy(alpha = 0.35f),
                                glass.orbMid.copy(alpha = 0.75f),
                                glass.accent.copy(alpha = 0.85f),
                            ),
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                        ),
                    )
                    // Inner lens edge.
                    drawCircle(
                        color = glass.orbCore.copy(alpha = 0.30f),
                        radius = this.size.minDimension * 0.33f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.5.dp.toPx(),
                        ),
                    )
                }
                .clickable(
                    interactionSource = connectInteractionSource,
                    indication = null,
                    enabled = canConnect || canDisconnect,
                    onClick = { if (canDisconnect) onDisconnect() else onConnect() },
                )
                .semantics {
                    contentDescription = "$actionLabel. $statusText"
                },
            contentAlignment = Alignment.Center,
        ) {
            // The inner disc carries the power glyph.
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                glass.orbCore.copy(alpha = 0.55f),
                                glass.orbMid.copy(alpha = 0.30f),
                            ),
                        ),
                        CircleShape,
                    )
                    .scale(1f + pulse.value * 0.04f),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_kx_power),
                    contentDescription = null,
                    tint = if (canConnect || canDisconnect) glass.textPrimary else glass.textSecondary,
                    modifier = Modifier.size(84.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(state)
            Spacer(modifier = Modifier.width(KXDimens.SpaceXs))
            Text(
                text = statusText,
                style = KXTypography.Title.copy(color = glass.textPrimary),
            )
        }
    }
}

@Composable
private fun StatusDot(state: TunnelState) {
    val color = when (state) {
        TunnelState.CONNECTED -> Color(0xFF4ADE80)
        TunnelState.FAILED, TunnelState.REVOKED -> KXPalette.NodeOrchid
        TunnelState.CONNECTING, TunnelState.RECONNECTING,
        TunnelState.PREPARING_PERMISSION, TunnelState.AWAITING_PERMISSION,
        TunnelState.DISCONNECTING,
        -> KXPalette.Cyan

        else -> KXPalette.NodeOrchid
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun FailureNotice(reason: FailureReason) {
    val glass = LocalKXGlass.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = KXDimens.CardInnerCorner,
    ) {
        Text(
            text = stringResource(failureLabel(reason)),
            style = KXTypography.Body.copy(color = glass.textPrimary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The location card. The reference shows "Best Location"; there is no location
 * concept until TASK_03, so the honest wording is used and the card says where the
 * feature is coming from.
 */
@Composable
private fun ChoiceCard() {
    val glass = LocalKXGlass.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.ic_kx_layers),
                contentDescription = null,
                tint = glass.accent,
                modifier = Modifier.size(38.dp),
            )
            Spacer(modifier = Modifier.width(KXDimens.SpaceMd))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_section_choice),
                    style = KXTypography.Caption.copy(color = glass.textSecondary),
                )
                Text(
                    text = stringResource(R.string.home_no_location_chosen),
                    style = KXTypography.Title.copy(color = glass.textPrimary),
                )
                Text(
                    text = stringResource(R.string.home_location_pending),
                    style = KXTypography.Caption.copy(color = glass.textSecondary),
                )
            }
        }
    }
}

/**
 * The four metric tiles.
 *
 * Every value is `--` until something real measures it. The session's byte counters
 * are real when a tunnel is up, and they are shown in the upload/download tiles at
 * that point — but nothing here is derived from a timer, a fake peer or an assumed
 * speed.
 */
@Composable
private fun MetricGrid(session: TunnelSessionState) {
    val stats = session.stats
    Row(horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceMd)) {
        MetricTile(
            iconRes = R.drawable.ic_kx_metric_ping,
            label = stringResource(R.string.home_metric_ping),
            value = stringResource(R.string.home_metric_unknown),
            unit = stringResource(R.string.home_metric_ping_unit),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            iconRes = R.drawable.ic_kx_metric_download,
            label = stringResource(R.string.home_metric_download),
            value = stats?.let { formatBytes(it.totalRx) }
                ?: stringResource(R.string.home_metric_unknown),
            unit = stringResource(R.string.home_metric_transfer_rate,
                session.trafficRate?.let { com.kx.vpn.domain.tunnel.formatTrafficRate(it.receivedBytesPerSecond) }
                    ?: stringResource(R.string.home_metric_unknown)),
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(KXDimens.SpaceMd))
    Row(horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceMd)) {
        MetricTile(
            iconRes = R.drawable.ic_kx_metric_upload,
            label = stringResource(R.string.home_metric_upload),
            value = stats?.let { formatBytes(it.totalTx) }
                ?: stringResource(R.string.home_metric_unknown),
            unit = stringResource(R.string.home_metric_transfer_rate,
                session.trafficRate?.let { com.kx.vpn.domain.tunnel.formatTrafficRate(it.sentBytesPerSecond) }
                    ?: stringResource(R.string.home_metric_unknown)),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            iconRes = R.drawable.ic_kx_shield_mask,
            label = stringResource(R.string.home_metric_ip_masking),
            value = stringResource(R.string.home_metric_unknown),
            unit = stringResource(R.string.home_metric_unavailable),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricTile(
    iconRes: Int,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current
    GlassCard(
        modifier = modifier.aspectRatio(1.75f),
        cornerRadius = KXDimens.CardInnerCorner,
        contentPadding = KXDimens.SpaceMd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = glass.accent,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(KXDimens.SpaceSm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = KXTypography.Caption.copy(color = glass.textSecondary),
                    maxLines = 1,
                )
                // The value and its unit are separate lines: combining them let a long
                // unit wrap under the value and collide with the tile below.
                Text(
                    text = value,
                    style = KXTypography.Title.copy(color = glass.textPrimary),
                    maxLines = 1,
                )
                Text(
                    text = unit,
                    style = KXTypography.Caption.copy(color = glass.textSecondary),
                    maxLines = 2,
                )
            }
        }
    }
}

/** Human-readable byte count. Real counters only — never a synthesised speed. */
private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024L * 1024 * 1024)} GB"
}

private fun statusLabel(state: TunnelState): Int = when (state) {
    TunnelState.UNCONFIGURED -> R.string.home_status_unconfigured
    TunnelState.DISCONNECTED -> R.string.home_status_disconnected
    TunnelState.PREPARING_PERMISSION -> R.string.home_status_preparing_permission
    TunnelState.AWAITING_PERMISSION -> R.string.home_status_awaiting_permission
    TunnelState.CONNECTING -> R.string.home_status_connecting
    TunnelState.CONNECTED -> R.string.home_status_connected
    TunnelState.DISCONNECTING -> R.string.home_status_disconnecting
    TunnelState.FAILED -> R.string.home_status_failed
    TunnelState.RECONNECTING -> R.string.home_status_reconnecting
    TunnelState.REVOKED -> R.string.home_status_revoked
}

private fun failureLabel(reason: FailureReason): Int = when (reason) {
    FailureReason.PermissionDenied -> R.string.home_failure_permission_denied
    FailureReason.BackendUnavailable -> R.string.home_failure_backend_unavailable
    FailureReason.InvalidConfiguration -> R.string.home_failure_invalid_configuration
    FailureReason.TunnelStartFailed -> R.string.home_failure_tunnel_start
    FailureReason.NetworkLost -> R.string.home_failure_network_lost
    FailureReason.RevokedBySystem -> R.string.home_failure_revoked
    FailureReason.AlreadyInUse -> R.string.home_failure_already_in_use
}
