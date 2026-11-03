package com.kx.vpn.data

import com.kx.vpn.domain.tunnel.formatSessionDuration
import com.kx.vpn.platform.TunnelStats
import org.junit.Assert.*
import org.junit.Test

class SessionDurationTest {
    private class Platform : TunnelPlatform {
        var readsFail = false
        override fun requestConsent() = ConsentRequest.NotRequired
        override fun startTunnel(configText: String) = Unit
        override fun stopTunnel() = Unit
        override fun isTunnelUp() = true
        override fun statistics(): TunnelStats? {
            if (readsFail) error("synthetic counter failure")
            return null
        }
    }

    @Test fun durationStartsOnlyWithBackendUpAndDoesNotCountConnectionAttempt() {
        var now = 10_000L
        val manager = TunnelSessionManager(Platform(), { now }) { "NONSECRET-TEST-CONFIG" }
        assertNull(manager.state.value.elapsedMillis)
        manager.connect(); now += 8_000; manager.refreshElapsedTime()
        assertNull(manager.state.value.elapsedMillis)
        manager.onPlatformStateChanged(true)
        assertEquals(0L, manager.state.value.elapsedMillis)
        assertEquals(now, manager.state.value.startedAtElapsedMillis)
        now += 3_456; manager.refreshElapsedTime()
        assertEquals(3_456L, manager.state.value.elapsedMillis)
    }

    @Test fun missedTicksAreMeasuredAndRepeatedUpDoesNotRestartTimer() {
        var now = 2_000L
        val manager = TunnelSessionManager(Platform(), { now }) { "NONSECRET-TEST-CONFIG" }
        manager.connect(); manager.onPlatformStateChanged(true)
        now += 3_600_000; manager.onPlatformStateChanged(true); manager.refreshElapsedTime()
        assertEquals(3_600_000L, manager.state.value.elapsedMillis)
        assertEquals("01:00:00", formatSessionDuration(manager.state.value.elapsedMillis!!))
    }

    @Test fun disconnectClearsTimeAndReconnectStartsANewInterval() {
        var now = 10L
        val manager = TunnelSessionManager(Platform(), { now }) { "NONSECRET-TEST-CONFIG" }
        manager.connect(); manager.onPlatformStateChanged(true)
        now += 2_000; manager.refreshElapsedTime()
        manager.disconnect()
        assertNull(manager.state.value.elapsedMillis)
        assertNull(manager.state.value.startedAtElapsedMillis)
        manager.onPlatformStateChanged(false)
        now += 50_000; manager.connect(); manager.onPlatformStateChanged(true)
        assertEquals(0L, manager.state.value.elapsedMillis)
        assertEquals(now, manager.state.value.startedAtElapsedMillis)
    }

    @Test fun missingCountersDoNotStopElapsedTimeAndRecreationCannotInventIt() {
        var now = 100L
        val platform = Platform().apply { readsFail = true }
        val manager = TunnelSessionManager(platform, { now }) { "NONSECRET-TEST-CONFIG" }
        manager.connect(); manager.onPlatformStateChanged(true)
        now += 1_000; manager.refreshStatistics()
        assertEquals(1_000L, manager.state.value.elapsedMillis)
        assertNull(manager.state.value.stats)
        val recreated = TunnelSessionManager(platform, { now }) { "NONSECRET-TEST-CONFIG" }
        assertNull(recreated.state.value.elapsedMillis)
    }

    @Test fun defensiveBackwardClockCannotDecreaseDisplayedDuration() {
        var now = 100L
        val manager = TunnelSessionManager(Platform(), { now }) { "NONSECRET-TEST-CONFIG" }
        manager.connect(); manager.onPlatformStateChanged(true)
        now = 2_100; manager.refreshElapsedTime()
        now = 50; manager.refreshElapsedTime()
        assertEquals(2_000L, manager.state.value.elapsedMillis)
    }

    @Test fun formatterUsesHoursWithoutTwentyFourHourWrapOrFalseRounding() {
        assertEquals("00:00:00", formatSessionDuration(999))
        assertEquals("00:01:01", formatSessionDuration(61_000))
        assertEquals("25:00:00", formatSessionDuration(90_000_000))
        var rejected = false
        try { formatSessionDuration(-1) } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }
}
