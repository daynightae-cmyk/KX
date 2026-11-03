package com.kx.vpn.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.backend.NoopTunnelActionHandler
import com.wireguard.config.Config
import com.kx.vpn.MainActivity
import com.kx.vpn.R

/**
 * The KX tunnel service.
 *
 * This is a real Android `VpnService`: the platform interface is established here, the
 * WireGuard backend brings the interface up, and `onRevoke` is handled because Android
 * can take the permission away at any time — including when the user revokes it from
 * system settings — and the app must not keep claiming a session it no longer has.
 *
 * Commands arrive as service intents rather than direct method calls, because
 * `VpnService.establish()` may only be called from inside the service, and a direct
 * call from the UI would race the service's own creation.
 */
class KXVpnService : GoBackend.VpnService(), Tunnel {

    private var backend: GoBackend? = null

    /**
     * Notified whenever the backend reports the tunnel up or down.
     *
     * Named differently from [Tunnel.onStateChange] on purpose: the interface method
     * is `onStateChange(Tunnel.State)` and a property of the same name collides with
     * it. The interface method below adapts the library's enum to this boolean.
     */
    var onBackendStateChanged: ((up: Boolean) -> Unit)? = null

    override fun onStateChange(state: Tunnel.State) {
        onBackendStateChanged?.invoke(state == Tunnel.State.UP)
    }

    override fun getName(): String = TUNNEL_NAME

    override fun onCreate() {
        super.onCreate()
        backend = runCatching {
            // The library's handler contract is four hook methods for PreUp/PostUp/
            // PreDown/PostDown. KX does not run user-supplied hook commands, so the
            // no-op handler is the correct choice rather than an invented one.
            GoBackend(this, NoopTunnelActionHandler())
        }.getOrNull()
        // A backend that failed to load is reported, not hidden: the session manager
        // turns this into FAILED with a real reason instead of pretending the tunnel is
        // merely still connecting.
        if (backend == null) {
            onBackendStateChanged?.invoke(false)
        }
        TunnelServiceRegistry.attach(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val configText = intent.getStringExtra(EXTRA_CONFIG)
                if (configText == null) {
                    onBackendStateChanged?.invoke(false)
                } else {
                    // startForeground must happen before the work: the platform gives a
                    // started foreground service only a few seconds to post its
                    // notification, and a backend that throws would otherwise leave it
                    // with none and get the process killed.
                    startForeground(NOTIFICATION_ID, buildNotification())
                    startTunnel(configText)
                }
            }

            ACTION_STOP -> stopTunnel()
        }
        // START_STICKY is wrong here: the service must not be resurrected with no
        // configuration, because a resurrected service with no profile has nothing to
        // bring up and would look like a phantom session.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        TunnelServiceRegistry.detach(this)
        onBackendStateChanged = null
        backend = null
        super.onDestroy()
    }

    /**
     * Android revoked our VPN consent. The session is gone whether we wanted it or not,
     * so the UI must be told immediately.
     */
    override fun onRevoke() {
        onBackendStateChanged?.invoke(false)
        super.onRevoke()
    }

    fun startTunnel(configText: String) {
        val currentBackend = backend
        if (currentBackend == null) {
            onBackendStateChanged?.invoke(false)
            return
        }
        val config = try {
            Config.Builder()
                .parseInterface(configText.lines())
                .parsePeer(configText.lines())
                .build()
        } catch (invalid: Exception) {
            // A malformed configuration must not take the service down.
            onBackendStateChanged?.invoke(false)
            return
        }

        try {
            currentBackend.setState(this, Tunnel.State.UP, config)
        } catch (failure: BackendException) {
            onBackendStateChanged?.invoke(false)
            return
        } catch (failure: Exception) {
            onBackendStateChanged?.invoke(false)
            return
        }

        // The backend reports the true state; only a real UP is forwarded as one.
        onBackendStateChanged?.invoke(true)
    }

    fun stopTunnel() {
        val currentBackend = backend ?: return
        runCatching { currentBackend.setState(this, Tunnel.State.DOWN, null) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        onBackendStateChanged?.invoke(false)
    }

    /** Real byte counters from the backend. Null when there is no live session. */
    fun statistics(): TunnelStats? {
        val currentBackend = backend ?: return null
        return runCatching {
            val stats: Statistics = currentBackend.getStatistics(this) ?: return null
            TunnelStats(totalRx = stats.totalRx(), totalTx = stats.totalTx())
        }.getOrNull()
    }

    fun backendVersion(): String? = runCatching { backend?.getVersion() }.getOrNull()

    /**
     * The backend's own view of the tunnel. This is a *query*, not a cached flag: the
     * answer comes from the WireGuard backend every time it is asked.
     */
    fun isTunnelUp(): Boolean {
        val currentBackend = backend ?: return false
        return runCatching {
            currentBackend.getState(this) == Tunnel.State.UP
        }.getOrDefault(false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.vpn_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.vpn_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kx_notification)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.kx.vpn.action.START_TUNNEL"
        const val ACTION_STOP = "com.kx.vpn.action.STOP_TUNNEL"
        const val EXTRA_CONFIG = "config"

        const val CHANNEL_ID = "kx_tunnel"
        const val NOTIFICATION_ID = 0x4B58
        const val TUNNEL_NAME = "kx0"

        /**
         * Asks the platform for VPN consent.
         *
         * Returns null when consent already exists, and an Intent the user must approve
         * otherwise. This is the only place the app may ask for VPN access, and it is
         * only ever called from a connect action — never automatically at onboarding.
         */
        fun requestConsent(context: Context): Intent? = VpnService.prepare(context)

        /**
         * The consent intent, or null when consent already exists.
         *
         * `VpnService.prepare()` returns an activity intent that the app launches and
         * that reports its outcome through `onActivityResult`. It is launched as-is
         * rather than being unwrapped: the nested EXTRA_INTENT form is an internal
         * detail, and reading it wrongly fails silently.
         */
        fun prepareConsent(context: Context): Intent? = VpnService.prepare(context)
    }
}

/** Real counters. Null values mean "not measured", never zero-as-a-claim. */
data class TunnelStats(val totalRx: Long, val totalTx: Long)
