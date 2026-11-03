package com.kx.vpn

import android.app.Application
import android.content.Context
import com.kx.vpn.data.ConnectedSessionTracker
import com.kx.vpn.data.EndpointCatalogueRepository
import com.kx.vpn.data.EndpointSwitchController
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.data.ProfileRepository
import com.kx.vpn.data.TunnelSessionManager
import com.kx.vpn.data.createLaunchStateRepository
import com.kx.vpn.data.vault.FileProfileBlobStore
import com.kx.vpn.data.vault.KeystoreAesGcmCipher
import com.kx.vpn.data.vault.ProfileVault
import com.kx.vpn.platform.AndroidNetworkAvailability
import com.kx.vpn.platform.AndroidTunnelPlatform
import com.kx.vpn.platform.NetworkAvailability
import com.kx.vpn.platform.TcpLatencyProbe
import com.kx.vpn.platform.TunnelServiceRegistry
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

private val Context.preferencesDataStore by preferencesDataStore(name = "kx_catalogue")
private val Context.securityPreferencesDataStore by preferencesDataStore(name = "kx_security")

/**
 * Application-scoped service locator, kept deliberately small.
 *
 * TASK_00 had one dependency to own. TASK_02 adds three more — the launch store, the
 * encrypted profile vault and the tunnel session — and they are all process-scoped for
 * the same reason: the VPN service lives in this process and has to be able to report
 * state back to the UI without a framework. TASK_03 adds the endpoint catalogue and the
 * switch that applies the user's server choice to the tunnel.
 */
class KXApplication : Application() {

    /**
     * Declared first because the lazily-created services below capture it.
     *
     * Main dispatcher: the switch's effects surface the platform consent request, and
     * only the UI layer can show it.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val launchStateRepository: LaunchStateRepository by lazy {
        createLaunchStateRepository(this)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            ProfileVault(
                cipher = KeystoreAesGcmCipher(),
                store = FileProfileBlobStore(File(filesDir, "profiles")),
            ),
        )
    }

    /**
     * The endpoint catalogue. It holds metadata only — a label, an optional country,
     * a host and a port — and never the configuration itself.
     */
    val securitySettingsRepository: com.kx.vpn.data.SecuritySettingsRepository by lazy {
        com.kx.vpn.data.PreferencesSecuritySettingsRepository(this.securityPreferencesDataStore)
    }

    val endpointCatalogueRepository: EndpointCatalogueRepository by lazy {
        EndpointCatalogueRepository(this.preferencesDataStore)
    }

    /** Real reachability measurement. Nothing here estimates a latency. */
    val latencyProbe: TcpLatencyProbe by lazy { TcpLatencyProbe() }

    /** The platform's own answer about internet reachability, for honest offline states. */
    val networkAvailability: NetworkAvailability by lazy {
        AndroidNetworkAvailability(this)
    }

    val tunnelSessionManager: TunnelSessionManager by lazy {
        TunnelSessionManager(
            platform = AndroidTunnelPlatform(this),
            profileSource = {
                val raw = profileRepository.activeConfigText()
                if (raw != null) {
                    val sec = kotlinx.coroutines.runBlocking { securitySettingsRepository.settings.first() }
                    com.kx.vpn.domain.config.VpnConfigModifier.applySecuritySettings(raw, sec)
                } else null
            },
        )
    }

    /**
     * Applies the user's server choice to the tunnel.
     */
    val endpointSwitchController: EndpointSwitchController by lazy {
        EndpointSwitchController(
            sessionManager = tunnelSessionManager,
            profiles = profileRepository,
            catalogue = endpointCatalogueRepository,
            scope = applicationScope,
        )
    }

    /**
     * The connected screen's numbers: a monotonic session timer and throughput derived
     * from the tunnel's own byte counters.
     *
     * `elapsedRealtime` is the monotonic source, which is the point: a user changing the
     * device clock, an NTP correction or a daylight-saving jump must not move the session
     * timer, and must not make a rate negative.
     */
    val connectedSessionTracker: ConnectedSessionTracker by lazy {
        ConnectedSessionTracker(
            sessionManager = tunnelSessionManager,
            catalogue = endpointCatalogueRepository,
            profiles = profileRepository,
            probe = latencyProbe,
            elapsedRealtime = android.os.SystemClock::elapsedRealtime,
            scope = applicationScope,
        )
    }

    override fun onCreate() {
        super.onCreate()
        // The service reports tunnel state changes back through the registry; the
        // manager turns them into state machine events.
        TunnelServiceRegistry.listener = { up ->
            tunnelSessionManager.onPlatformStateChanged(up)
        }
        tunnelSessionManager.refreshProfilePresence()
        // The chosen endpoint, not merely "some profile", decides what the tunnel uses.
        // Asynchronously: a cold start must not block the main thread on a disk read.
        applicationScope.launch { restoreActiveEndpoint() }
    }

    /**
     * Points the profile repository at the endpoint the user chose last time.
     *
     * The choice lives in the catalogue (metadata only) and the configuration lives in
     * the vault, so on a cold start they have to be re-joined. When the stored choice no
     * longer resolves to a profile, the first profile that does is used instead — and if
     * none does, the app stays unconfigured rather than guessing.
     */
    private suspend fun restoreActiveEndpoint() {
        val catalogue = runCatching { endpointCatalogueRepository.state.first() }.getOrNull()
        val chosen = catalogue?.activeEndpointId
            ?.let { id -> catalogue.endpoints.firstOrNull { it.id == id } }
        if (chosen != null && profileRepository.activate(chosen.profileId)) {
            tunnelSessionManager.refreshProfilePresence()
            return
        }
        profileRepository.knownProfileIds().firstOrNull()?.let { profileRepository.activate(it) }
        tunnelSessionManager.refreshProfilePresence()
    }
}
