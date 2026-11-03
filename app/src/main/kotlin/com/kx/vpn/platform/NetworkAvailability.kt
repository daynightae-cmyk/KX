package com.kx.vpn.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the device currently has a usable network.
 *
 * This exists so the server screen can say "there is no network" instead of showing a
 * measurement spinner that can never finish. It reads the platform's own answer; it does
 * not probe, guess or cache a previous result.
 */
interface NetworkAvailability {
    /** Live online state, emitted whenever the platform's answer changes. */
    val isOnline: Flow<Boolean>

    /** The answer right now, for a one-shot decision before starting work. */
    fun currentlyOnline(): Boolean
}

class AndroidNetworkAvailability(
    context: Context,
) : NetworkAvailability {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    override fun currentlyOnline(): Boolean {
        val manager = connectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Registers a real callback and unregisters it when the collector goes away, so a
     * screen that stops observing stops holding a platform registration.
     *
     * `NET_CAPABILITY_VALIDATED` is what makes this "the network can reach the internet"
     * rather than "an interface is up" — connected-but-not-validated is exactly the state
     * in which a latency probe would hang.
     */
    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(currentlyOnline()) }
            override fun onLost(network: Network) { trySend(currentlyOnline()) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(currentlyOnline())
            }
        }
        trySend(currentlyOnline())
        manager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback,
        )
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
