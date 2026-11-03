package com.kx.vpn.data

import com.kx.vpn.domain.tunnel.FailureReason
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.platform.TunnelStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The session manager's job is to make commands idempotent and races safe while
 * keeping the state machine as the single source of truth.
 */
class TunnelSessionManagerTest {

    private class FakePlatform(
        var consentRequired: Boolean = false,
    ) : TunnelPlatform {
        var startCalls = 0
        var stopCalls = 0
        var lastConfig: String? = null
        var up = false
        var stats: TunnelStats? = null

        override fun requestConsent(): ConsentRequest =
            if (consentRequired) ConsentRequest.Required("req-1") else ConsentRequest.NotRequired

        override fun startTunnel(configText: String) {
            startCalls++
            lastConfig = configText
        }

        override fun stopTunnel() {
            stopCalls++
            up = false
        }

        override fun isTunnelUp(): Boolean = up

        override fun statistics(): TunnelStats? = stats
    }

    private val config = """
        [Interface]
        PrivateKey = abc
        Address = 10.8.0.2/32

        [Peer]
        PublicKey = def
        AllowedIPs = 0.0.0.0/0
    """.trimIndent()

    private fun manager(
        platform: FakePlatform = FakePlatform(),
        withProfile: Boolean = true,
    ) = TunnelSessionManager(
        platform = platform,
        profileSource = { if (withProfile) config else null },
    )

    // -- Configuration ----------------------------------------------------------------

    @Test
    fun withoutAProfileTheSessionIsUnconfiguredAndCannotConnect() {
        val platform = FakePlatform()
        val manager = manager(platform, withProfile = false)

        assertEquals(TunnelState.UNCONFIGURED, manager.state.value.tunnelState)
        assertFalse(manager.state.value.canConnect)

        manager.connect()
        assertEquals("no platform work may happen without a profile", 0, platform.startCalls)
        assertEquals(TunnelState.UNCONFIGURED, manager.state.value.tunnelState)
    }

    @Test
    fun aProfileMovesTheSessionToDisconnected() {
        val manager = manager()
        assertEquals(TunnelState.DISCONNECTED, manager.state.value.tunnelState)
        assertTrue(manager.state.value.canConnect)
    }

    // -- Consent -----------------------------------------------------------------------

    @Test
    fun connectWithConsentAlreadyGrantedGoesStraightToStarting() {
        val platform = FakePlatform(consentRequired = false)
        val manager = manager(platform)

        manager.connect()

        assertEquals(TunnelState.CONNECTING, manager.state.value.tunnelState)
        assertEquals(1, platform.startCalls)
        assertEquals(config, platform.lastConfig)
    }

    @Test
    fun connectNeedingConsentWaitsForTheUser() {
        val platform = FakePlatform(consentRequired = true)
        val manager = manager(platform)
        var consent: ConsentRequest? = null
        manager.onConsentRequired = { consent = it }

        manager.connect()

        assertEquals(TunnelState.AWAITING_PERMISSION, manager.state.value.tunnelState)
        assertEquals(0, platform.startCalls)
        assertEquals(ConsentRequest.Required("req-1"), consent)
    }

    @Test
    fun grantingConsentStartsTheTunnel() {
        val platform = FakePlatform(consentRequired = true)
        val manager = manager(platform)

        manager.connect()
        manager.onPermissionGranted()

        assertEquals(TunnelState.CONNECTING, manager.state.value.tunnelState)
        assertEquals(1, platform.startCalls)
    }

    @Test
    fun denyingConsentFailsWithThatReasonAndStartsNothing() {
        val platform = FakePlatform(consentRequired = true)
        val manager = manager(platform)

        manager.connect()
        manager.onPermissionDenied()

        assertEquals(TunnelState.FAILED, manager.state.value.tunnelState)
        assertEquals(FailureReason.PermissionDenied, manager.state.value.failure)
        assertEquals(0, platform.startCalls)
    }

    // -- Idempotency ------------------------------------------------------------------

    @Test
    fun repeatedConnectTapsProduceOneStart() {
        val platform = FakePlatform()
        val manager = manager(platform)

        manager.connect()
        manager.connect()
        manager.connect()

        assertEquals("a second tap must not start a second tunnel", 1, platform.startCalls)
    }

    @Test
    fun repeatedDisconnectTapsProduceOneStop() {
        val platform = FakePlatform()
        val manager = manager(platform)

        manager.connect()
        manager.onPlatformStateChanged(up = true)
        assertEquals(TunnelState.CONNECTED, manager.state.value.tunnelState)

        manager.disconnect()
        manager.disconnect()

        assertEquals(1, platform.stopCalls)
    }

    @Test
    fun connectingTwiceWhileAlreadyConnectedIsANoOp() {
        val platform = FakePlatform()
        val manager = manager(platform)
        manager.connect()
        manager.onPlatformStateChanged(up = true)

        manager.connect()

        assertEquals(TunnelState.CONNECTED, manager.state.value.tunnelState)
        assertEquals(1, platform.startCalls)
    }

    // -- Platform truth ----------------------------------------------------------------

    @Test
    fun connectedOnlyAfterThePlatformSaysSo() {
        val platform = FakePlatform()
        val manager = manager(platform)

        manager.connect()
        assertFalse("connecting is not connected", manager.state.value.isTunnelUp)

        manager.onPlatformStateChanged(up = true)
        assertTrue(manager.state.value.isTunnelUp)
        assertEquals(TunnelState.CONNECTED, manager.state.value.tunnelState)
    }

    @Test
    fun thePlatformReportingDownReturnsToDisconnected() {
        val platform = FakePlatform()
        val manager = manager(platform)
        manager.connect()
        manager.onPlatformStateChanged(up = true)

        manager.onPlatformStateChanged(up = false)

        assertEquals(TunnelState.DISCONNECTED, manager.state.value.tunnelState)
        assertFalse(manager.state.value.isTunnelUp)
    }

    @Test
    fun aFailureFromTheBackendIsRecordedWithItsReason() {
        val platform = FakePlatform()
        val manager = manager(platform)
        manager.connect()

        manager.onBackendFailure(FailureReason.BackendUnavailable)

        assertEquals(TunnelState.FAILED, manager.state.value.tunnelState)
        assertEquals(FailureReason.BackendUnavailable, manager.state.value.failure)
    }

    @Test
    fun aProfileThatVanishesWhileWaitingForConsentFailsInsteadOfStartingAnEmptyTunnel() {
        // Consent is required, so the session really does wait: the profile is removed
        // while the consent sheet is on screen, and the start must then refuse rather
        // than bring up a tunnel with no configuration.
        val platform = FakePlatform(consentRequired = true)
        var profile: String? = config
        val manager = TunnelSessionManager(platform, profileSource = { profile })

        manager.connect()
        assertEquals(TunnelState.AWAITING_PERMISSION, manager.state.value.tunnelState)

        profile = null
        manager.onPermissionGranted()

        assertEquals(TunnelState.FAILED, manager.state.value.tunnelState)
        assertEquals(FailureReason.InvalidConfiguration, manager.state.value.failure)
        assertEquals("no tunnel may be started without a configuration", 0, platform.startCalls)
    }

    // -- Revocation and network churn --------------------------------------------------

    @Test
    fun revocationIsReflectedImmediately() {
        val platform = FakePlatform()
        val manager = manager(platform)
        manager.connect()
        manager.onPlatformStateChanged(up = true)

        manager.onRevoked()

        assertEquals(TunnelState.REVOKED, manager.state.value.tunnelState)
        assertEquals(FailureReason.RevokedBySystem, manager.state.value.failure)
        assertFalse(manager.state.value.isTunnelUp)
    }

    @Test
    fun losingTheNetworkMovesToReconnectingAndRestoringItRestarts() {
        val platform = FakePlatform()
        val manager = manager(platform)
        manager.connect()
        manager.onPlatformStateChanged(up = true)

        manager.onNetworkLost()
        assertEquals(TunnelState.RECONNECTING, manager.state.value.tunnelState)

        manager.onNetworkRestored()
        assertEquals(TunnelState.CONNECTING, manager.state.value.tunnelState)
        assertEquals(2, platform.startCalls)
    }

    // -- Statistics --------------------------------------------------------------------

    @Test
    fun statisticsAreOnlyCollectedWhileTheTunnelIsUp() {
        val platform = FakePlatform()
        val manager = manager(platform)

        platform.stats = TunnelStats(totalRx = 100, totalTx = 50)
        manager.refreshStatistics()
        assertNull("no live session means no counters", manager.state.value.stats)

        manager.connect()
        manager.onPlatformStateChanged(up = true)
        manager.refreshStatistics()
        assertEquals(TunnelStats(100, 50), manager.state.value.stats)

        manager.onPlatformStateChanged(up = false)
        manager.refreshStatistics()
        assertNull(manager.state.value.stats)
    }

    @Test
    fun tunnelUpIsNotTheSameAsBeingProtected() {
        val platform = FakePlatform()
        val manager = manager(platform)
        manager.connect()
        manager.onPlatformStateChanged(up = true)
        assertTrue(manager.state.value.isTunnelUp)

        manager.onBackendFailure(FailureReason.NetworkLost)
        assertFalse(manager.state.value.isTunnelUp)
    }
}
