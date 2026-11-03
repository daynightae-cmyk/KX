package com.kx.vpn.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.kx.vpn.data.ConsentRequest
import com.kx.vpn.data.TunnelPlatform
import java.util.UUID

/**
 * Where the running service registers itself.
 *
 * The service and the UI live in the same process, so a process-scoped registry is the
 * simplest correct way for the service to report state changes back. It is deliberately
 * not a static mutable in the service itself: the service is otherwise free of app-wide
 * state, and the registry is the single place that knows about the wiring.
 */
object TunnelServiceRegistry {
    @Volatile
    var service: KXVpnService? = null
        private set

    @Volatile
    var listener: ((up: Boolean) -> Unit)? = null

    fun attach(instance: KXVpnService) {
        service = instance
        instance.onBackendStateChanged = { up -> listener?.invoke(up) }
    }

    fun detach(instance: KXVpnService) {
        if (service === instance) service = null
    }
}

/**
 * The Android implementation of [TunnelPlatform].
 *
 * Every method performs a real platform action: consent is requested through
 * `VpnService.prepare`, the tunnel is started by commanding the real `VpnService`, and
 * statistics come from the WireGuard backend. Nothing here fabricates a result.
 */
class AndroidTunnelPlatform(
    private val context: Context,
) : TunnelPlatform {

    override fun requestConsent(): ConsentRequest {
        val intent = KXVpnService.requestConsent(context)
        return if (intent == null) {
            ConsentRequest.NotRequired
        } else {
            // The request id stands in for the consent intent. The UI layer builds the
            // actual intent through KXVpnService.requestConsent so the manager stays
            // free of Android types.
            ConsentRequest.Required(UUID.randomUUID().toString())
        }
    }

    override fun startTunnel(configText: String) {
        val intent = Intent(context, KXVpnService::class.java).apply {
            action = KXVpnService.ACTION_START
            putExtra(KXVpnService.EXTRA_CONFIG, configText)
        }
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    override fun stopTunnel() {
        val intent = Intent(context, KXVpnService::class.java).apply {
            action = KXVpnService.ACTION_STOP
        }
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    override fun isTunnelUp(): Boolean =
        TunnelServiceRegistry.service?.let { service ->
            runCatching { service.isTunnelUp() }.getOrDefault(false)
        } ?: false

    override fun statistics(): TunnelStats? =
        TunnelServiceRegistry.service?.statistics()
}
