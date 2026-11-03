package com.kx.vpn.ui.imports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kx.vpn.R
import com.kx.vpn.domain.config.BoundedFileReader
import com.kx.vpn.domain.config.ConfigIssue
import com.kx.vpn.domain.config.ConfigValidation
import com.kx.vpn.domain.config.WireGuardConfigValidator
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.KXAtmosphericBackground
import com.kx.vpn.ui.brand.KXPillButton
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXPalette
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * First-run configuration entry.
 *
 * This is a real, working path: pick a file, read it under a hard byte ceiling, parse
 * it strictly, and report what was actually found. What it deliberately does *not* do
 * is pretend the profile is now active — there is no tunnel engine in this build, so
 * the apply control is disabled with the reason stated on screen rather than hidden or
 * faked. Secure storage of the profile is TASK_02; the full import workflow is TASK_09.
 *
 * Nothing is uploaded and nothing is persisted here.
 */
@Composable
fun ImportEntryScreen(
    viewModel: ImportEntryViewModel,
    onSaveProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val glass = LocalKXGlass.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) {
            // A cancelled picker is not an error and must not look like one.
            viewModel.reset()
            return@rememberLauncherForActivityResult
        }
        viewModel.onReadStarted()
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                val stream = runCatching {
                    context.contentResolver.openInputStream(uri)
                }.getOrNull()
                    ?: return@withContext BoundedFileReader.Outcome.Failed(
                        BoundedFileReader.FailureReason.Unreadable,
                    )
                BoundedFileReader.read(stream, WireGuardConfigValidator.MAX_CONFIG_BYTES)
            }
            viewModel.onBytesRead(outcome)
        }
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
                text = stringResource(R.string.import_title),
                style = KXTypography.Display.copy(color = glass.textPrimary),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceSm))

            Text(
                text = stringResource(R.string.import_explainer),
                style = KXTypography.Body.copy(color = glass.textSecondary),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

            KXPillButton(
                label = stringResource(
                    if (state is ImportUiState.Reading) {
                        R.string.import_reading
                    } else {
                        R.string.import_choose_file
                    },
                ),
                onClick = { picker.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) },
                enabled = state !is ImportUiState.Reading,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

            when (val current = state) {
                is ImportUiState.Idle, is ImportUiState.Reading -> Unit

                is ImportUiState.Unreadable -> SimpleCard(
                    headline = stringResource(R.string.import_unreadable),
                    detail = stringResource(
                        if (current.reason == BoundedFileReader.FailureReason.Empty) {
                            R.string.import_unreadable_empty
                        } else {
                            R.string.import_unreadable_denied
                        },
                    ),
                    tone = KXPalette.NodeOrchid,
                )

                is ImportUiState.Rejected -> IssueCard(
                    headline = stringResource(R.string.import_rejected),
                    issues = current.issues,
                    tone = KXPalette.NodeOrchid,
                )

                is ImportUiState.Inspected -> InspectedCard(
                    summary = current.summary,
                    configText = current.configText,
                    onSave = onSaveProfile,
                )
            }

            Spacer(modifier = Modifier.height(KXDimens.SpaceLg))

            Text(
                text = stringResource(R.string.import_privacy_note),
                style = KXTypography.Caption.copy(color = KXPalette.NeutralHint),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(KXDimens.SpaceXl))
        }
    }
}

@Composable
private fun SimpleCard(
    headline: String,
    detail: String,
    tone: androidx.compose.ui.graphics.Color,
) {
    val glass = LocalKXGlass.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(KXDimens.SpaceXs)) {
            Text(text = headline, style = KXTypography.Caption.copy(color = tone))
            Text(
                text = detail,
                style = KXTypography.Body.copy(color = glass.textPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IssueCard(
    headline: String,
    issues: List<ConfigIssue>,
    tone: androidx.compose.ui.graphics.Color,
) {
    val glass = LocalKXGlass.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(KXDimens.SpaceXs)) {
            Text(text = headline, style = KXTypography.Caption.copy(color = tone))
            // One row per finding, resolved in a composable context. Joining them into a
            // single string would require calling stringResource inside a plain lambda,
            // which the compiler rejects — and any workaround there would quietly drop
            // localisation.
            issues.forEach { issue ->
                Text(
                    text = "• ${issue.field}: " +
                        stringResource(ConfigIssueMessages.of(issue.messageKey)),
                    style = KXTypography.Body.copy(color = glass.textPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * The truthful result of a successful parse. Public keys and endpoints are shown because
 * they are public by construction; the private key is never named, quoted or counted
 * beyond a boolean.
 */
@Composable
private fun InspectedCard(
    summary: com.kx.vpn.domain.config.TunnelSummary,
    configText: String,
    onSave: (String) -> Unit,
) {
    val glass = LocalKXGlass.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(KXDimens.SpaceSm)) {
            Text(
                text = stringResource(R.string.import_accepted),
                style = KXTypography.Caption.copy(color = glass.accent),
            )

            FactRow(
                label = stringResource(R.string.import_fact_private_key),
                value = stringResource(
                    if (summary.interfaceHasPrivateKey) {
                        R.string.import_fact_present
                    } else {
                        R.string.import_fact_absent
                    },
                ),
            )
            FactRow(
                label = stringResource(R.string.import_fact_peers),
                value = summary.peerCount.toString(),
            )
            FactRow(
                label = stringResource(R.string.import_fact_endpoints),
                value = summary.peerEndpoints.joinToString().ifEmpty {
                    stringResource(R.string.import_fact_none)
                },
            )
            FactRow(
                label = stringResource(R.string.import_fact_routes),
                value = if (summary.routesAllTraffic) {
                    stringResource(R.string.import_fact_routes_all)
                } else {
                    stringResource(R.string.import_fact_routes_selective, summary.allowedIpCount)
                },
            )
            FactRow(
                label = stringResource(R.string.import_fact_dns),
                value = summary.interfaceDnsServers.joinToString().ifEmpty {
                    stringResource(R.string.import_fact_dns_system)
                },
            )

            // Saving is a real action: the configuration goes into the encrypted
            // profile vault, and the home screen can then connect with it. Connecting
            // itself happens from home, not from here.
            KXPillButton(
                label = stringResource(R.string.import_save),
                onClick = { onSave(configText) },
                emphasized = false,
                modifier = Modifier.padding(top = KXDimens.SpaceSm),
            )
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    val glass = LocalKXGlass.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = KXTypography.Caption.copy(color = glass.textSecondary),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = KXTypography.Body.copy(color = glass.textPrimary),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f),
        )
    }
}
