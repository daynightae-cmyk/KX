package com.kx.vpn.data

import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.platform.TunnelStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TunnelStatisticsSamplerTest {
    private class Platform : TunnelPlatform {
        var reads = 0
        var result: TunnelStats? = TunnelStats(120, 80)
        var beforeReturn: (() -> Unit)? = null
        var throws = false
        override fun requestConsent() = ConsentRequest.NotRequired
        override fun startTunnel(configText: String) = Unit
        override fun stopTunnel() = Unit
        override fun isTunnelUp() = true
        override fun statistics(): TunnelStats? {
            reads++
            beforeReturn?.invoke()
            if (throws) error("synthetic backend failure")
            return result
        }
    }
    private fun connected(platform: Platform): TunnelSessionManager =
        TunnelSessionManager(platform) { "NONSECRET-TEST-CONFIG" }.apply {
            connect(); onPlatformStateChanged(true)
        }

    @Test fun onlyUpSessionsArePolledAndDisconnectCancelsPolling() = runTest {
        val platform = Platform()
        val manager = TunnelSessionManager(platform) { "NONSECRET-TEST-CONFIG" }
        val sampling = launch { sampleTunnelStatistics(manager, sample = { manager.refreshStatistics() }) }
        runCurrent(); advanceTimeBy(2_000); runCurrent()
        assertEquals(0, platform.reads)
        manager.connect(); manager.onPlatformStateChanged(true); runCurrent()
        assertEquals(1, platform.reads)
        assertEquals(TunnelStats(120, 80), manager.state.value.stats)
        advanceTimeBy(1_000); runCurrent(); assertEquals(2, platform.reads)
        manager.disconnect(); runCurrent()
        assertNull(manager.state.value.stats)
        advanceTimeBy(3_000); runCurrent(); assertEquals(2, platform.reads)
        sampling.cancelAndJoin()
    }

    @Test fun oldCounterResultCannotSurviveDisconnectDuringRead() {
        val platform = Platform(); val manager = connected(platform)
        platform.beforeReturn = { manager.disconnect() }
        manager.refreshStatistics()
        assertEquals(TunnelState.DISCONNECTING, manager.state.value.tunnelState)
        assertNull(manager.state.value.stats)
        assertEquals(1, platform.reads)
    }

    @Test fun oldCounterResultCannotAttachToAReconnectedSession() {
        val platform = Platform(); val manager = connected(platform)
        platform.beforeReturn = {
            manager.disconnect(); manager.onPlatformStateChanged(false)
            manager.connect(); manager.onPlatformStateChanged(true)
        }
        manager.refreshStatistics()
        assertTrue(manager.state.value.isTunnelUp)
        assertNull(manager.state.value.stats)
    }

    @Test fun missingFailedOrNegativeReadClearsPriorMeasurementWithoutFakingZero() {
        val platform = Platform(); val manager = connected(platform)
        manager.refreshStatistics(); assertNotNull(manager.state.value.stats)
        platform.result = null; manager.refreshStatistics(); assertNull(manager.state.value.stats)
        platform.result = TunnelStats(-1, 0); manager.refreshStatistics(); assertNull(manager.state.value.stats)
        platform.result = TunnelStats(0, 0); manager.refreshStatistics()
        assertEquals(TunnelStats(0, 0), manager.state.value.stats) // Actual zero is valid.
        platform.throws = true; manager.refreshStatistics(); assertNull(manager.state.value.stats)
        assertTrue(manager.state.value.isTunnelUp) // Missing stats is not a fabricated DOWN.
    }

    @Test fun invalidSamplingFrequencyDoesNotStartBackendWork() = runTest {
        val platform = Platform(); val manager = connected(platform)
        var rejected = false
        try { sampleTunnelStatistics(manager, 0) } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected); assertEquals(0, platform.reads)
    }
}
