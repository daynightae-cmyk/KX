package com.kx.vpn.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kx.vpn.KXApplication
import com.kx.vpn.MainActivity
import com.kx.vpn.R
import com.kx.vpn.domain.session.SessionPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Foreground owner for the library VpnService. No automatic restart/connect. */
class TunnelSessionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository get() = (application as KXApplication).sessionRepository
    private var commanded = false

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.tunnel_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, TunnelSessionService::class.java).setAction(STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        startForeground(42, Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_kx_launcher_foreground)
            .setContentTitle(getString(R.string.tunnel_notification_title))
            .setContentText(getString(R.string.tunnel_notification_body))
            .setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.tunnel_disconnect), stop).build())
            .build())
        scope.launch {
            repository.state.collect { snapshot ->
                if (commanded && snapshot.phase in setOf(SessionPhase.Disconnected, SessionPhase.Failed)) stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            scope.launch { repository.disconnect(); stopSelf() }
        } else if (intent?.action == START && !commanded) {
            commanded = true
            scope.launch { repository.connect() }
        } else if (intent?.action != START) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        // App-owned cleanup survives destruction of this service's UI scope.
        repository.shutdown()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val START = "com.kx.vpn.START_OWNED_TUNNEL"
        const val STOP = "com.kx.vpn.STOP_OWNED_TUNNEL"
        private const val CHANNEL = "owned-vpn-session"
    }
}
