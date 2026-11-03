package com.kx.vpn.ui.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kx.vpn.R
import com.kx.vpn.domain.servers.ServerFilter
import com.kx.vpn.domain.servers.ServerRow
import com.kx.vpn.domain.servers.ServerSwitchFailure
import com.kx.vpn.domain.servers.ServerSwitchState
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.IconAction
import com.kx.vpn.ui.brand.KXAtmosphericBackground
import com.kx.vpn.ui.brand.KXButton
import com.kx.vpn.ui.brand.KXButtonStyle
import com.kx.vpn.ui.brand.KXOrb
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass

/**
 * The server selection screen, the native implementation of
 * `assets/reference/03-server-selection.png`.
 *
 * The reference artwork shows seven countries with latencies and "Load" percentages.
 * This build shows **only endpoints the user actually owns**, and only latencies that
 * were really measured. Where the reference shows a load figure there is no honest
 * source for one, so that column is absent rather than invented, and the capability
 * filters that need metadata nobody supplied are disabled with the reason stated.
 *
 * Freshness is read from [ServerListState.generatedAtMillis] — the same instant the rows
 * were built for — so a row cannot show "fastest" and "--" at the same time.
 */
@Composable
fun SelectServerScreen(
    state: ServerListState,
    query: String,
    selectedFilter: ServerFilter,
    enabledFilters: Set<ServerFilter>,
    isMeasuring: Boolean,
    isOffline: Boolean,
    switchState: ServerSwitchState,
    switchFailure: ServerSwitchFailure?,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ServerFilter) -> Unit,
    onSelect: (ServerRow) -> Unit,
    onToggleFavorite: (ServerRow) -> Unit,
    onDismissSwitchFailure: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalKXGlass.current

    KXAtmosphericBackground(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // The brand header is 168dp of orb plus a display title. In landscape, or on a
            // short viewport, that alone fills the screen and the user's servers are pushed
            // off the bottom — so the header gives way rather than the list disappearing.
            val compact = maxHeight < CompactViewportLimit
            val orbSize = if (compact) 88.dp else 168.dp
            val headerGap = if (compact) KXDimens.SpaceXs else KXDimens.SpaceMd

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KXDimens.ScreenPadding, vertical = KXDimens.SpaceSm),
                ) {
                    IconAction(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.servers_back),
                        onClick = onBack,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconAction(
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.home_tab_settings),
                        onClick = { },
                        enabled = false,
                    )
                }

                KXOrb(size = orbSize)

                Spacer(modifier = Modifier.height(headerGap))

                Text(
                    text = stringResource(R.string.servers_title),
                    style = (if (compact) KXTypography.Title else KXTypography.Display)
                        .copy(color = glass.textPrimary),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(headerGap))

                SearchField(query = query, onQueryChange = onQueryChange)

                Spacer(modifier = Modifier.height(KXDimens.SpaceSm))

                FilterChips(
                    selected = selectedFilter,
                    enabled = enabledFilters,
                    onSelect = onFilterChange,
                )

                Spacer(modifier = Modifier.height(KXDimens.SpaceSm))

                // A switch in flight is reported where the user asked for it, and a failed
                // one stays on screen until they acknowledge it. Neither is a toast that can
                // be missed while the list looks unchanged.
                SwitchBanner(
                    switchState = switchState,
                    failure = switchFailure,
                    onDismiss = onDismissSwitchFailure,
                )

                when {
                    state.rows.isEmpty() -> EmptyCatalogue(onImport = onImport)
                    else -> LazyColumn(
                        // Takes what is left and scrolls, so the header can never push the
                        // user's servers off the screen.
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = KXDimens.ScreenPadding,
                            vertical = KXDimens.SpaceXs,
                        ),
                        verticalArrangement = Arrangement.spacedBy(KXDimens.SpaceXs),
                    ) {
                        if (isOffline) {
                            // Stated instead of implied: with no validated network there is
                            // nothing to measure, and a spinner would be a lie of patience.
                            item(key = "offline") {
                                ListNotice(stringResource(R.string.servers_offline))
                            }
                        } else if (isMeasuring) {
                            item(key = "measuring") { ListNotice(stringResource(R.string.servers_measuring)) }
                        }
                        items(state.rows, key = { it.profile.id }) { row ->
                            ServerRowCard(
                                row = row,
                                nowMillis = state.generatedAtMillis,
                                isActive = row.profile.id == state.activeEndpointId,
                                isSwitching = switchState.isInFlight(),
                                onSelect = { onSelect(row) },
                                onToggleFavorite = { onToggleFavorite(row) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Below this height the brand header shrinks so the list stays reachable. */
private val CompactViewportLimit = 560.dp

/** Label, glyph and the reason a filter cannot be used, resolved from resources. */
private fun ServerFilter.labelRes(): Int = when (this) {
    ServerFilter.All -> R.string.servers_filter_all
    ServerFilter.Fastest -> R.string.servers_filter_fastest
    ServerFilter.Streaming -> R.string.servers_filter_streaming
    ServerFilter.Gaming -> R.string.servers_filter_gaming
}

private fun ServerFilter.iconRes(): Int = when (this) {
    ServerFilter.All -> R.drawable.ic_kx_filter_all
    ServerFilter.Fastest -> R.drawable.ic_kx_filter_fastest
    ServerFilter.Streaming -> R.drawable.ic_kx_filter_streaming
    ServerFilter.Gaming -> R.drawable.ic_kx_filter_gaming
}

private fun ServerFilter.disabledReason(): Int = when (this) {
    ServerFilter.Streaming -> R.string.servers_filter_streaming_unavailable
    ServerFilter.Gaming -> R.string.servers_filter_gaming_unavailable
    else -> R.string.servers_filter_unavailable
}

private fun ServerSwitchState.isInFlight(): Boolean = when (this) {
    ServerSwitchState.DISCONNECTING_OLD,
    ServerSwitchState.APPLYING_NEW,
    ServerSwitchState.AWAITING_PERMISSION,
    ServerSwitchState.CONNECTING,
    ServerSwitchState.ROLLING_BACK,
    -> true

    else -> false
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val glass = LocalKXGlass.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KXDimens.ScreenPadding)
            // A minimum rather than a fixed height, so a large system text size grows the
            // field instead of clipping the user's query.
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(glass.glassFillBrush, RoundedCornerShape(percent = 50))
            .padding(horizontal = KXDimens.SpaceMd),
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(R.drawable.ic_kx_search),
            contentDescription = null,
            tint = glass.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(KXDimens.SpaceSm))
        // Resolved outside the semantics block: stringResource is a composable call
        // and cannot be made from inside a semantics lambda.
        val searchDescription = stringResource(R.string.servers_search_hint)
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = KXTypography.Body.copy(color = glass.textPrimary),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = searchDescription },
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.servers_search_hint),
                        style = KXTypography.Body.copy(color = glass.textSecondary),
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun FilterChips(
    selected: ServerFilter,
    enabled: Set<ServerFilter>,
    onSelect: (ServerFilter) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KXDimens.SpaceXs),
        modifier = Modifier
            .fillMaxWidth()
            // Scrollable, so a fourth filter is never silently cut off at the edge of a
            // narrow phone — the row simply continues.
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KXDimens.ScreenPadding),
    ) {
        ServerFilter.entries.forEach { filter ->
            FilterChip(
                filter = filter,
                isSelected = filter == selected,
                isEnabled = filter in enabled,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    filter: ServerFilter,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val glass = LocalKXGlass.current
    val label = stringResource(filter.labelRes())
    // Resolved in a composable scope, not inside the semantics lambda below.
    val disabledText = stringResource(filter.disabledReason())
    val chipInteractionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    val background = when {
        isSelected -> Brush.linearGradient(
            listOf(KXPalette.Violet.copy(alpha = 0.95f), KXPalette.Cyan.copy(alpha = 0.85f)),
        )

        else -> Brush.linearGradient(
            listOf(glass.glassTop.copy(alpha = if (isEnabled) 0.8f else 0.35f),
                glass.glassBottom.copy(alpha = if (isEnabled) 0.8f else 0.35f)),
        )
    }
    val contentColor = when {
        isSelected -> KXPalette.NightTextPrimary
        isEnabled -> glass.textPrimary
        else -> glass.textSecondary.copy(alpha = 0.55f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background, RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = chipInteractionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick,
            )
            .semantics {
                contentDescription = if (isEnabled) label else "$label. $disabledText"
            }
            .padding(horizontal = KXDimens.SpaceMd, vertical = 10.dp),
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(filter.iconRes()),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(KXDimens.SpaceXs))
        Text(
            text = label,
            style = KXTypography.Caption.copy(color = contentColor),
            maxLines = 1,
        )
    }
}

@Composable
private fun ServerRowCard(
    row: ServerRow,
    nowMillis: Long,
    isActive: Boolean,
    isSwitching: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val glass = LocalKXGlass.current
    val rowInteractionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    val measurement = row.measurement
    // Both come from the same instant the rows were built for, so the badge, the number
    // and the stale marker can never contradict each other.
    val latency = measurement?.displayLatency(nowMillis)
    val isStale = measurement != null && !measurement.isFresh(nowMillis)
    val activeChoice = stringResource(R.string.servers_chosen)
    val switchingHint = stringResource(R.string.servers_switching)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = rowInteractionSource,
                indication = null,
                enabled = !isSwitching,
                onClick = onSelect,
            )
            .semantics {
                contentDescription = buildString {
                    append(row.profile.displayName)
                    if (latency != null) append(", ${latency}ms")
                    if (isActive) append(", $activeChoice")
                    if (isSwitching) append(", $switchingHint")
                }
            },
        cornerRadius = KXDimens.CardInnerCorner,
        contentPadding = KXDimens.SpaceMd,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(glass.orbCore.copy(alpha = 0.30f), glass.orbMid.copy(alpha = 0.18f)),
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.profile.flag ?: "·",
                    style = KXTypography.Title.copy(color = glass.textPrimary),
                )
            }

            Spacer(modifier = Modifier.width(KXDimens.SpaceMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.profile.displayName,
                    style = KXTypography.Title.copy(color = glass.textPrimary),
                    maxLines = 1,
                )
                // "Chosen" describes the user's selection, not a connection. Saying
                // "connected" here would claim something this screen cannot observe.
                if (isActive) {
                    Text(
                        text = if (isSwitching) switchingHint else activeChoice,
                        style = KXTypography.Caption.copy(color = glass.accent),
                    )
                } else if (row.isRecommended) {
                    Text(
                        text = stringResource(R.string.servers_recommended),
                        style = KXTypography.Caption.copy(color = glass.accent),
                    )
                }
                // The host is the user's own endpoint and is not a secret, but it is
                // still the user's infrastructure, so it is shown small and secondary.
                Text(
                    text = row.profile.host,
                    style = KXTypography.Caption.copy(color = glass.textSecondary),
                    maxLines = 1,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (latency != null) {
                    // The reference prefixes a measured latency with a bolt. The glyph is
                    // decoration for a number that really was timed, so it is kept; the
                    // "--" branch below deliberately has no bolt, because nothing was
                    // measured and the icon would imply otherwise.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            painter = painterResource(R.drawable.ic_kx_filter_fastest),
                            contentDescription = null,
                            tint = glass.latency,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$latency ms",
                            style = KXTypography.Caption.copy(color = glass.latency),
                        )
                    }
                } else {
                    // Nothing truthful to show. "--" is the reference's own marker for
                    // an unknown value and is used here for the same reason.
                    Text(
                        text = stringResource(R.string.servers_latency_unknown),
                        style = KXTypography.Caption.copy(color = glass.textSecondary),
                    )
                }
                if (isStale) {
                    Text(
                        text = stringResource(R.string.servers_measurement_stale),
                        style = KXTypography.Caption.copy(color = glass.textSecondary),
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.width(KXDimens.SpaceSm))

            FavoriteButton(isFavorite = row.isFavorite, onClick = onToggleFavorite)
        }
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    val glass = LocalKXGlass.current
    // Both of these are resolved in a composable scope: stringResource cannot be
    // called from inside a semantics lambda, and an inline MutableInteractionSource
    // allocates on every recomposition.
    val description = stringResource(
        if (isFavorite) R.string.servers_unfavorite else R.string.servers_favorite,
    )
    val interactionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    Box(
        modifier = Modifier
            .size(KXDimens.MinTouchTarget)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(
                if (isFavorite) R.drawable.ic_kx_star_filled else R.drawable.ic_kx_star,
            ),
            contentDescription = null,
            // The filled and outlined shapes already differ, so the tint follows the
            // theme accent rather than a fixed cyan that would vanish on the day surface.
            tint = if (isFavorite) glass.accent else glass.textSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Progress and failure for the switch the user asked for.
 *
 * A successful switch needs no banner: the row itself starts saying "Chosen". A failure
 * does, and it stays until the user dismisses it — including the case where the switch
 * already rolled back and the state machine is idle again. The outcome is in the failure,
 * not in the state, so the banner reads the failure.
 */
@Composable
private fun SwitchBanner(
    switchState: ServerSwitchState,
    failure: ServerSwitchFailure?,
    onDismiss: () -> Unit,
) {
    val glass = LocalKXGlass.current
    val dismissInteractionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    if (switchState.isInFlight()) return
    if (failure == null && switchState != ServerSwitchState.FAILED) return
    val message = stringResource(failure?.messageRes() ?: R.string.servers_switch_failed_generic)
    val dismiss = stringResource(R.string.servers_switch_dismiss)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KXDimens.ScreenPadding, vertical = KXDimens.SpaceXs)
            .clip(RoundedCornerShape(KXDimens.CardInnerCorner))
            .background(glass.glassFillBrush, RoundedCornerShape(KXDimens.CardInnerCorner))
            .padding(horizontal = KXDimens.SpaceMd, vertical = KXDimens.SpaceSm)
            .semantics { contentDescription = message },
    ) {
        Text(
            text = message,
            style = KXTypography.Caption.copy(color = glass.textPrimary),
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(KXDimens.SpaceSm))
        Text(
            text = dismiss,
            style = KXTypography.Caption.copy(color = glass.accent),
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .clickable(
                    interactionSource = dismissInteractionSource,
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(horizontal = KXDimens.SpaceSm, vertical = KXDimens.SpaceXs),
        )
    }
}

private fun ServerSwitchFailure.messageRes(): Int = when (this) {
    ServerSwitchFailure.PermissionDenied -> R.string.servers_switch_failed_permission
    ServerSwitchFailure.ApplyFailed -> R.string.servers_switch_failed_apply
    ServerSwitchFailure.ConnectFailed -> R.string.servers_switch_failed_connect
    ServerSwitchFailure.RollbackFailed -> R.string.servers_switch_failed_rollback
    ServerSwitchFailure.UnknownEndpoint -> R.string.servers_switch_failed_unknown
    ServerSwitchFailure.Busy -> R.string.servers_switch_failed_busy
}

@Composable
private fun ListNotice(text: String) {
    val glass = LocalKXGlass.current
    Text(
        text = text,
        style = KXTypography.Body.copy(color = glass.textSecondary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = KXDimens.SpaceSm),
        textAlign = TextAlign.Center,
    )
}

/**
 * The empty catalogue.
 *
 * The honest answer to "no servers" is an invitation to add one, not a fabricated
 * list of popular countries.
 */
@Composable
private fun EmptyCatalogue(onImport: () -> Unit) {
    val glass = LocalKXGlass.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KXDimens.ScreenPadding, vertical = KXDimens.SpaceLg),
    ) {
        Text(
            text = stringResource(R.string.servers_empty_title),
            style = KXTypography.Title.copy(color = glass.textPrimary),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(KXDimens.SpaceXs))
        Text(
            text = stringResource(R.string.servers_empty_body),
            style = KXTypography.Body.copy(color = glass.textSecondary),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(KXDimens.SpaceLg))
        KXButton(
            label = stringResource(R.string.servers_empty_import),
            onClick = onImport,
            style = KXButtonStyle.Secondary,
        )
    }
}
