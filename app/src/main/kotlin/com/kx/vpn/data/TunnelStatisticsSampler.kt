package com.kx.vpn.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Samples real backend totals only while UP. No probe/network request or speed claim. */
suspend fun sampleTunnelStatistics(
    manager: TunnelSessionManager,
    intervalMillis: Long = 1_000L,
    sample: suspend () -> Unit = { withContext(Dispatchers.IO) { manager.refreshStatistics() } },
) {
    require(intervalMillis >= 250L) { "Counter sampling interval is too short" }
    manager.state.map { it.isTunnelUp }.distinctUntilChanged().collectLatest { up ->
        if (up) {
            while (currentCoroutineContext().isActive) {
                sample()
                delay(intervalMillis)
            }
        }
    }
}
