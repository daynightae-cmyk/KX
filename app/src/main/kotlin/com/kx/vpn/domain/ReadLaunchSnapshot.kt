package com.kx.vpn.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Bounds the initial preferences read, not just the platform splash window.
 *
 * A silent/empty store is unknown, never proof of a fresh or accepted setup. The
 * existing degraded-storage destination explains that limitation without deleting
 * preferences. Parent cancellation and programming failures must still propagate.
 */
internal suspend fun readLaunchSnapshot(
    snapshots: Flow<LaunchSnapshot>,
    timeoutMillis: Long = 3_000L,
): LaunchSnapshot {
    require(timeoutMillis > 0) { "Startup deadline must be positive" }
    return withTimeoutOrNull(timeoutMillis) { snapshots.firstOrNull() }
        ?: LaunchSnapshot(onboardingCompleted = false, storageDegraded = true)
}
