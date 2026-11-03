package com.kx.vpn.ui.connected

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kx.vpn.R
import com.kx.vpn.data.ConnectedSessionState
import com.kx.vpn.domain.session.SessionTelemetry
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.KXButton
import com.kx.vpn.ui.brand.KXButtonStyle
import com.kx.vpn.ui.brand.crestPath
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * The connected session screen — `assets/reference/04-connected-session.png`.
 *
 * The reference shows 92.4 Mbps, 46.8 Mbps, 28 ms, a session of 00:42:18, an assigned
 * public IP of 91.107.182.204 and a UAE → Germany route. **None of those are reproducible
 * here**, so this screen shows what the device actually knows:
 *
 *  * the headline appears only when the backend says the tunnel interface is up;
 *  * a monotonic session timer, or `--` before it starts;
 *  * throughput derived from the tunnel's own byte counters over a real window, or `--`
 *    when the window cannot be trusted;
 *  * a ping measured against the user's own endpoint, with the instant it was taken;
 *  * the tunnel address from the user's own configuration, labelled as such — the public
 *    exit address is stated as **not verified**, because determining it needs an external
 *    service that has not been given consent;
 *  * a route card whose left side is "this device", because no location service was
 *    consulted and a country here would be invented.
 *
 * The word "protected" never appears on this screen, and neither does a shield that
 * implies it.
 */
@Composable
fun ConnectedSessionContent(
    tunnelState: TunnelState,
    session: ConnectedSessionState,
    onDisconnect: () -> Unit,
    onChangeServer: () -> Unit,
    onPingAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The headline follows the backend lifecycle and nothing else. The telemetry state has
    // its own `isTunnelUp` mirror; reading the session manager's state here is what keeps
    // "Connected" from ever being drawn on a screen whose tunnel is not up.
    val up = tunnelState == TunnelState.CONNECTED
    val headline = if (up) {
        stringResource(R.string.home_status_connected)
    } else {
        stringResource(tunnelState.headlineRes())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KXDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConnectionRing(
            up = up,
            stateLabel = headline,
            durationLabel = SessionTelemetry.formatDuration(session.elapsedMillis),
        )

        Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

        RouteCard(session = session)

        Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

        Row(horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceSm)) {
            val download = SessionTelemetry.rateParts(session.throughput?.downloadBytesPerSecond)
            val upload = SessionTelemetry.rateParts(session.throughput?.uploadBytesPerSecond)
            MetricTile(
                label = stringResource(R.string.connected_download),
                value = download.first,
                unit = download.second,
                detail = session.rxBytes?.let {
                    stringResource(R.string.connected_total_received, SessionTelemetry.formatBytes(it))
                } ?: stringResource(R.string.connected_no_sample),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = stringResource(R.string.connected_upload),
                value = upload.first,
                unit = upload.second,
                detail = session.txBytes?.let {
                    stringResource(R.string.connected_total_sent, SessionTelemetry.formatBytes(it))
                } ?: stringResource(R.string.connected_no_sample),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = stringResource(R.string.connected_ping),
                value = session.pingMillis?.toString() ?: stringResource(R.string.connected_latency_unknown),
                unit = if (session.pingMillis != null) "ms" else "",
                detail = if (session.pingFailed) {
                    stringResource(R.string.connected_ping_unreachable)
                } else {
                    stringResource(R.string.connected_ping_target)
                },
                onClick = onPingAgain,
                clickLabel = stringResource(R.string.connected_measure_again),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(KXDimens.SpaceSm))

        Row(horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceSm)) {
            MetricTile(
                label = stringResource(R.string.connected_duration),
                value = SessionTelemetry.formatDuration(session.elapsedMillis),
                unit = "",
                detail = stringResource(R.string.connected_duration_monotonic),
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = stringResource(R.string.connected_tunnel_address),
                value = session.tunnelAddress ?: stringResource(R.string.connected_latency_unknown),
                unit = "",
                detail = stringResource(R.string.connected_tunnel_address_from_config),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(KXDimens.SpaceMd))

        // The precise claim. "VPN UP" and "internet verified" are required by
        // docs/SECURITY_MODEL.md to stay distinct, so this line says which one is true and
        // which one has not been established.
        VerificationNotice(session = session)

        Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

        KXButton(
            label = stringResource(R.string.connected_disconnect),
            onClick = onDisconnect,
            style = KXButtonStyle.Primary,
            fillWidth = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(KXDimens.SpaceSm))

        KXButton(
            label = stringResource(R.string.connected_change_server),
            onClick = onChangeServer,
            style = KXButtonStyle.Secondary,
            fillWidth = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(KXDimens.SpaceLg))
    }
}

private fun TunnelState.headlineRes(): Int = when (this) {
    TunnelState.UNCONFIGURED -> R.string.home_status_unconfigured
    TunnelState.DISCONNECTED -> R.string.home_status_disconnected
    TunnelState.PREPARING_PERMISSION -> R.string.home_status_preparing_permission
    TunnelState.AWAITING_PERMISSION -> R.string.home_status_awaiting_permission
    TunnelState.CONNECTING -> R.string.home_status_connecting
    TunnelState.CONNECTED -> R.string.home_status_connected
    TunnelState.DISCONNECTING -> R.string.home_status_disconnecting
    TunnelState.RECONNECTING -> R.string.home_status_reconnecting
    TunnelState.FAILED -> R.string.home_status_failed
    TunnelState.REVOKED -> R.string.home_status_revoked
}

@Composable
private fun ConnectionRing(
    up: Boolean,
    stateLabel: String,
    durationLabel: String,
) {
    val glass = LocalKXGlass.current
    val ringDescription = stringResource(R.string.connected_ring_description, stateLabel, durationLabel)

    Box(
        modifier = Modifier
            .size(248.dp)
            .semantics { contentDescription = ringDescription },
        contentAlignment = Alignment.Center,
    ) {
        // matchParentSize, not fillMaxWidth: a Canvas has no intrinsic height, so
        // fillMaxWidth alone gave it zero and the ring drew nothing at all.
        Canvas(modifier = Modifier.matchParentSize()) {
            val ringWidth = ConnectionRingGeometry.ringStrokeWidth(size.minDimension)
            val radius = (size.minDimension - ringWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val alpha = if (up) 1f else 0.45f

            // Outer luminous ring. A full ring, not an arc: an arc would read as progress
            // toward a completion nobody is computing.
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        glass.accent.copy(alpha = 0.25f * alpha),
                        KXPalette.Cyan.copy(alpha = 0.95f * alpha),
                        KXPalette.Orchid.copy(alpha = 0.85f * alpha),
                        glass.accent.copy(alpha = 0.25f * alpha),
                    ),
                    center = center,
                ),
                radius = radius,
                center = center,
                style = Stroke(width = ringWidth),
            )
            // Inner halo so the ring sits in glass rather than floating on the backdrop.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.orbCore.copy(alpha = 0.35f * alpha),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )

            val shield = ConnectionRingGeometry.crestBounds(size.minDimension)
            // `crestPath` takes the horizontal centre and absolute top/bottom, so the
            // bounds are already canvas coordinates and need no further offsetting.
            val path = crestPath(
                center = center,
                halfWidth = shield.halfWidth,
                top = shield.top,
                bottom = shield.bottom,
            )
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    listOf(KXPalette.Violet.copy(alpha = 0.95f * alpha), KXPalette.Cyan.copy(alpha = 0.85f * alpha)),
                ),
            )
            if (up) {
                // The check exists only while the backend says the interface is up.
                drawPath(
                    path = ConnectionRingGeometry.checkPath(center, size.minDimension),
                    color = KXPalette.NightTextPrimary,
                    style = Stroke(width = ringWidth * 0.9f),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Text(
                text = stateLabel,
                style = KXTypography.Title.copy(color = glass.textPrimary),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(KXDimens.SpaceXs))
            TimerPill(durationLabel)
            Spacer(modifier = Modifier.height(KXDimens.SpaceMd))
        }
    }
}

@Composable
private fun TimerPill(durationLabel: String) {
    val glass = LocalKXGlass.current
    val label = stringResource(R.string.connected_session_timer, durationLabel)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(glass.glassFillBrush, RoundedCornerShape(percent = 50))
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = durationLabel,
            style = KXTypography.Body.copy(color = glass.textPrimary),
            modifier = Modifier.padding(horizontal = KXDimens.SpaceMd, vertical = KXDimens.SpaceXs),
        )
    }
}

@Composable
private fun RouteCard(session: ConnectedSessionState) {
    val glass = LocalKXGlass.current
    val endpoint = session.endpoint
    // The left side is deliberately "this device". Naming a country for the user's own
    // location would require a geolocation service, and no such service was consulted.
    val origin = stringResource(R.string.connected_route_origin)
    val destination = endpoint?.displayName ?: stringResource(R.string.connected_route_none)
    val description = stringResource(R.string.connected_route_description, origin, destination)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        cornerRadius = KXDimens.CardInnerCorner,
        contentPadding = KXDimens.SpaceMd,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = origin,
                style = KXTypography.Body.copy(color = glass.textSecondary),
            )
            Spacer(modifier = Modifier.width(KXDimens.SpaceMd))
            // A drawable rather than a "→" character: the row mirrors in RTL, and a text
            // arrow would then point from the destination back at the origin.
            Icon(
                painter = painterResource(R.drawable.ic_kx_route_arrow),
                contentDescription = null,
                tint = glass.accent,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(KXDimens.SpaceMd))
            Text(
                text = destination,
                style = KXTypography.Body.copy(color = glass.textPrimary),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * One traffic tile.
 *
 * The figure and its unit are separate text elements on one line because a three-across
 * row at 360dp cannot hold "11.5 Mbit/s" at display size, and clipping to "11.5" would
 * drop the only part that makes the number mean anything.
 *
 * A tile with an action carries it on the whole card rather than a button inside it — an
 * embedded button wrapped "Measure again" to "Meas ure again" at this width.
 */
@Composable
private fun MetricTile(
    label: String,
    value: String,
    unit: String,
    detail: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    clickLabel: String = "",
) {
    val glass = LocalKXGlass.current
    val tileInteractionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    GlassCard(
        modifier = modifier
            .then(
                if (onClick == null) {
                    Modifier.semantics { contentDescription = "$label. $value $unit. $detail" }
                } else {
                    Modifier
                        .clickable(
                            interactionSource = tileInteractionSource,
                            indication = null,
                            onClick = onClick,
                        )
                        .semantics {
                            contentDescription = "$label. $value $unit. $detail. $clickLabel"
                        }
                },
            ),
        cornerRadius = KXDimens.CardInnerCorner,
        contentPadding = KXDimens.SpaceSm,
        highlight = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = KXDimens.SpaceXs)),
        ) {
            Text(
                text = label,
                style = KXTypography.Caption.copy(color = glass.textSecondary),
                maxLines = 1,
            )
            // One text element with two styles, not a Row of two Texts. A Row reversed by
            // RTL put the Latin unit before the number and clipped "Mbit/s" to "Mbit" at
            // 360dp; a single annotated string keeps bidi ordering correct and wraps
            // instead of truncating.
            Text(
                text = buildAnnotatedString {
                    withStyle(KXTypography.MetricValue.copy(color = glass.textPrimary).toSpanStyle()) {
                        append(value)
                    }
                    if (unit.isNotEmpty()) {
                        // Non-breaking: a plain space let the unit wrap away from its
                        // number, which reads as a measurement with no stated value.
                        append(' ')
                        withStyle(KXTypography.Caption.copy(color = glass.textSecondary).toSpanStyle()) {
                            append(unit)
                        }
                    }
                },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = value + " " + unit },
            )
            Text(
                text = detail,
                style = KXTypography.Caption.copy(color = glass.textSecondary),
                // Three lines, not two: at 360dp the second line truncated "Measured to
                // your server" to "Measured to your", which drops the only word that says
                // what the number refers to.
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun VerificationNotice(session: ConnectedSessionState) {
    val glass = LocalKXGlass.current
    val message = if (session.exitVerified) {
        stringResource(R.string.connected_exit_verified)
    } else {
        stringResource(R.string.connected_exit_not_verified)
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = message },
        cornerRadius = KXDimens.CardInnerCorner,
        contentPadding = KXDimens.SpaceSm,
        highlight = false,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.exitVerified) KXPalette.Cyan else glass.textSecondary,
                        CircleShape,
                    ),
            )
            Spacer(modifier = Modifier.width(KXDimens.SpaceSm))
            Text(
                text = message,
                style = KXTypography.Caption.copy(color = glass.textSecondary),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The ring's geometry, kept free of drawing calls so the numbers can be asserted.
 *
 * `minDimension` is the ring box in pixels; every value is derived from it so the emblem
 * scales with the viewport instead of being re-tuned per density.
 */
object ConnectionRingGeometry {

    /** Ring thickness, clamped so it stays visible on a small box and restrained on a large one. */
    fun ringStrokeWidth(minDimension: Float): Float =
        (minDimension * 0.035f).coerceIn(6f, 22f)

    data class CrestBounds(val top: Float, val bottom: Float, val halfWidth: Float) {
        val height: Float get() = bottom - top
    }

    /** The crest occupies the upper-middle of the ring, leaving room for the label below. */
    fun crestBounds(minDimension: Float): CrestBounds {
        val centre = minDimension / 2f
        val halfWidth = minDimension * 0.155f
        val top = centre - minDimension * 0.235f
        val bottom = centre + minDimension * 0.075f
        return CrestBounds(top = top, bottom = bottom, halfWidth = halfWidth)
    }

    /**
     * The check inside the crest, as a path.
     *
     * It sits above the vertical centre and is deliberately smaller than the crest,
     * because the crest converges to a point: a check sized to the crest's widest part
     * had its right arm poking outside the shield's edge, which is what the first render
     * showed.
     *
     * Returns an empty path for a non-positive box, because a degenerate canvas must not
     * produce coordinates that leak outside it.
     */
    fun checkPath(centre: Offset, minDimension: Float): Path {
        val path = Path()
        if (minDimension <= 0f || !minDimension.isFinite()) return path
        val unit = minDimension * 0.045f
        val anchorY = centre.y - minDimension * 0.06f
        val start = Offset(centre.x - unit * 1.5f, anchorY - unit * 0.55f)
        val elbow = Offset(centre.x - unit * 0.25f, anchorY + unit * 0.85f)
        val end = Offset(centre.x + unit * 1.75f, anchorY - unit * 1.35f)
        path.moveTo(start.x, start.y)
        path.lineTo(elbow.x, elbow.y)
        path.lineTo(end.x, end.y)
        return path
    }
}
