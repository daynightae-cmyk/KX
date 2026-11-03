package com.kx.vpn.data.session

import android.content.Context
import com.kx.vpn.domain.config.ConfigValidation
import com.kx.vpn.domain.config.WireGuardConfigValidator
import com.kx.vpn.domain.session.SessionController
import com.kx.vpn.domain.session.TunnelDriver
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Config/key material never enters StateFlow, exception messages or UI state. */
class WireGuardSessionRepository(context: Context) {
    private val store = EncryptedTunnelStore(context.applicationContext)
    private val backend by lazy { GoBackend(context.applicationContext) }
    private val operations = Mutex()
    private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var restored = false
    fun shutdown() { appScope.launch { operations.withLock { controller.disconnect(preserveFailure = true) } } }
    private val tunnel = object : Tunnel {
        override fun getName() = "kx-owned"
        override fun onStateChange(newState: Tunnel.State) {
            if (newState == Tunnel.State.DOWN) controller.backendDown()
        }
    }
    val controller: SessionController = SessionController(object : TunnelDriver {
        override suspend fun start(): Boolean = withContext(Dispatchers.IO) {
            val bytes = store.read()
            try {
                val config = bytes.inputStream().use { Config.parse(it) }
                backend.setState(tunnel, Tunnel.State.UP, config) == Tunnel.State.UP
            } finally { bytes.fill(0) }
        }
        override suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
            backend.setState(tunnel, Tunnel.State.DOWN, null) == Tunnel.State.DOWN
        }
    })
    val state = controller.state

    suspend fun restore() = operations.withLock {
        if (restored) return@withLock
        restored = true
        withContext(Dispatchers.IO) {
            // Presence is not a connection; decryption/validation happen on connect.
            if (store.exists()) controller.configured()
        }
    }

    suspend fun saveOwnedConfig(bytes: ByteArray): Boolean = operations.withLock {
        withContext(Dispatchers.IO) {
            if (state.value.problem == com.kx.vpn.domain.session.SessionProblem.CleanupRequired) return@withContext false
            if (state.value.phase !in setOf(
                    com.kx.vpn.domain.session.SessionPhase.Unconfigured,
                    com.kx.vpn.domain.session.SessionPhase.Disconnected,
                    com.kx.vpn.domain.session.SessionPhase.Failed,
                )) return@withContext false
            if (WireGuardConfigValidator.validate(bytes) !is ConfigValidation.Valid) return@withContext false
            // The actual engine parser must also accept it; the inspector is not a substitute.
            bytes.inputStream().use { Config.parse(it) }
            store.write(bytes)
            controller.configured()
            true
        }
    }

    suspend fun removeOwnedConfig(): Boolean = operations.withLock {
        controller.removeProfile {
            withContext(Dispatchers.IO) { store.delete(); true }
        }
    }

    suspend fun connect() = operations.withLock { controller.connect() }
    suspend fun disconnect() = operations.withLock { controller.disconnect() }
}
