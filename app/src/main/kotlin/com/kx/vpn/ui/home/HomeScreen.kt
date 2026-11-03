package com.kx.vpn.ui.home

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kx.vpn.R
import com.kx.vpn.domain.config.BoundedFileReader
import com.kx.vpn.domain.config.WireGuardConfigValidator
import com.kx.vpn.domain.session.SessionPhase
import com.kx.vpn.platform.TunnelSessionService
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.KXAtmosphericBackground
import com.kx.vpn.ui.brand.KXButton
import com.kx.vpn.ui.brand.KXButtonStyle
import com.kx.vpn.ui.brand.KXOrb
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXTypography
import com.kx.vpn.ui.theme.LocalKXGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val snapshot by viewModel.session.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val glass = LocalKXGlass.current
    val cleanupRequired = snapshot.problem == com.kx.vpn.domain.session.SessionProblem.CleanupRequired
    val active = cleanupRequired || snapshot.phase in setOf(SessionPhase.Up, SessionPhase.Connecting, SessionPhase.Disconnecting)
    fun start() {
        try {
            // Recheck consent immediately before dispatch, including revocation races.
            if (VpnService.prepare(context) != null) {
                viewModel.repository.controller.permissionDenied()
                return
            }
            ContextCompat.startForegroundService(context,
                Intent(context, TunnelSessionService::class.java).setAction(TunnelSessionService.START))
        } catch (_: Exception) { viewModel.actionFailed() }
    }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) start()
        else viewModel.repository.controller.permissionDenied()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importConfig {
            withContext(Dispatchers.IO) {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) BoundedFileReader.Outcome.Failed(BoundedFileReader.FailureReason.Unreadable)
                else BoundedFileReader.read(stream, WireGuardConfigValidator.MAX_CONFIG_BYTES)
            }
        }
    }
    KXAtmosphericBackground {
        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
            .padding(KXDimens.ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.home_title), Modifier.semantics { heading() },
                style = KXTypography.Display.copy(color = glass.textPrimary))
            KXOrb(size = KXDimens.OrbMedium)
            Text(stringResource(if (cleanupRequired) R.string.tunnel_cleanup_required else when (snapshot.phase) {
                SessionPhase.Unconfigured -> R.string.tunnel_unconfigured
                SessionPhase.Disconnected -> R.string.tunnel_disconnected
                SessionPhase.Connecting -> R.string.tunnel_connecting
                SessionPhase.Up -> R.string.tunnel_up
                SessionPhase.Disconnecting -> R.string.tunnel_disconnecting
                SessionPhase.Failed -> R.string.tunnel_failed
            }), color = glass.textPrimary, textAlign = TextAlign.Center)
            KXButton(
                label = stringResource(if (active) R.string.tunnel_disconnect else R.string.tunnel_connect),
                onClick = {
                    if (active) viewModel.disconnect()
                    else try {
                        val intent = VpnService.prepare(context)
                        if (intent == null) start() else consent.launch(intent)
                    } catch (_: Exception) { viewModel.actionFailed() }
                },
                enabled = !busy && snapshot.phase != SessionPhase.Unconfigured,
            )
            if (error || snapshot.problem != com.kx.vpn.domain.session.SessionProblem.None) {
                Text(stringResource(R.string.tunnel_action_error), color = glass.textPrimary)
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.tunnel_owned_title), color = glass.textPrimary)
                    Text(stringResource(R.string.tunnel_owned_body), color = glass.textSecondary)
                    KXButton(stringResource(R.string.tunnel_save_config), {
                        picker.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
                    }, enabled = !active && !busy, style = KXButtonStyle.Secondary)
                }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tunnel_measurements_unavailable), color = glass.textPrimary)
            }
            Text(stringResource(R.string.tunnel_limits), color = glass.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
        }
    }
}
