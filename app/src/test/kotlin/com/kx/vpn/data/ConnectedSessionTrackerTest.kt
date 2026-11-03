package com.kx.vpn.data

import com.kx.vpn.data.vault.AeadCipher
import com.kx.vpn.data.vault.ProfileBlobStore
import com.kx.vpn.data.vault.ProfileVault
import com.kx.vpn.domain.servers.EndpointMeasurement
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.platform.LatencyProbe
import com.kx.vpn.platform.TunnelStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The connected session's numbers, and the conditions under which they refuse to appear.
 *
 * A fake monotonic clock and a fake platform mean nothing here happens by accident: the
 * clock only moves when the test moves it, and the tunnel only comes up when the test says
 * the platform reported it up.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectedSessionTrackerTest {

    private class FakePlatform : TunnelPlatform {
        var up = false
        var stats: TunnelStats? = null

        override fun requestConsent(): ConsentRequest = ConsentRequest.NotRequired
        override fun startTunnel(configText: String) = Unit
        override fun stopTunnel() = Unit
        override fun isTunnelUp(): Boolean = up
        override fun statistics(): TunnelStats? = stats
    }

    private class ReversingCipher : AeadCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray? = plaintext.reversed().toByteArray()
        override fun decrypt(ciphertext: ByteArray): ByteArray? = ciphertext.reversed().toByteArray()
        override val isAvailable: Boolean get() = true
        override fun destroyKey() = Unit
    }

    private class InMemoryStore : ProfileBlobStore {
        private val blobs = mutableMapOf<String, ByteArray>()
        override fun write(id: String, bytes: ByteArray) { blobs[id] = bytes }
        override fun read(id: String): ByteArray? = blobs[id]
        override fun delete(id: String) { blobs.remove(id) }
        override fun list(): List<String> = blobs.keys.sorted()
        override fun clearAll() { blobs.clear() }
    }

    private class FakeProbe(var result: Map<String, EndpointMeasurement> = emptyMap()) : LatencyProbe {
        var calls = 0
        override suspend fun measure(profiles: List<EndpointProfile>): Map<String, EndpointMeasurement> {
            calls++
            return result
        }
    }

    private val config = """
        [Interface]
        PrivateKey = priv
        Address = 10.80.0.2/32, fdfd::2/128
        DNS = 10.80.0.1

        [Peer]
        PublicKey = pub
        Endpoint = 203.0.113.7:51820
        AllowedIPs = 0.0.0.0/0
    """.trimIndent()

    /**
     * Everything one test needs, with the clock and the platform under the test's control.
     *
     * The tracker runs on its own scope over the shared virtual-time scheduler, so its
     * sampling loop is driven by [advance] and cancelled explicitly rather than left
     * running.
     */
    private inner class Fixture(
        private val scheduler: TestCoroutineScheduler,
        intervalMillis: Long,
    ) {
        val platform = FakePlatform()
        val profiles = ProfileRepository(ProfileVault(cipher = ReversingCipher(), store = InMemoryStore()))
        val catalogue = EndpointCatalogueRepository(InMemoryPreferencesDataStore())
        val session = TunnelSessionManager(
            platform = platform,
            profileSource = { profiles.activeConfigText() },
        )
        val probe = FakeProbe()
        var clock = 100_000L

        private val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(scheduler))
        val tracker = ConnectedSessionTracker(
            sessionManager = session,
            catalogue = catalogue,
            profiles = profiles,
            probe = probe,
            elapsedRealtime = { clock },
            scope = scope,
            sampleIntervalMillis = intervalMillis,
        )

        init {
            profiles.save("p1", config)
            session.refreshProfilePresence()
        }

        /** Runs everything the scheduler has queued at the current virtual instant. */
        fun flush() {
            scheduler.runCurrent()
        }

        /**
         * Moves the monotonic clock and the virtual clock together, then lets the sampler
         * run. They move as one because that is the only way a real device's monotonic
         * clock behaves; a test that moved one without the other would be measuring the
         * fake rather than the code.
         */
        fun advance(millis: Long) {
            clock += millis
            scheduler.advanceTimeBy(millis)
            scheduler.runCurrent()
        }

        suspend fun seedEndpoint() {
            catalogue.upsert(
                EndpointProfile(
                    id = "e1",
                    displayName = "Home gateway",
                    countryCode = null,
                    host = "203.0.113.7",
                    port = 51820,
                    profileId = "p1",
                ),
            )
        }

        /** Brings the tunnel up the way the app does: connect, then the platform answers. */
        fun bringUp() {
            session.connect()
            platform.up = true
            session.onPlatformStateChanged(up = true)
            flush()
        }

        fun bringDown() {
            platform.up = false
            session.onPlatformStateChanged(up = false)
            flush()
        }

        fun teardown() {
            scope.cancel()
        }
    }

    private fun fixture(
        intervalMillis: Long = 2_000L,
        withEndpoint: Boolean = true,
        body: suspend Fixture.() -> Unit,
    ) = runTest {
        val harness = Fixture(testScheduler, intervalMillis)
        if (withEndpoint) harness.seedEndpoint()
        harness.flush()
        try {
            harness.body()
            harness.flush()
        } finally {
            harness.teardown()
        }
    }

    @Test
    fun nothingIsReportedWhileTheTunnelIsDown() = fixture {
        assertNull(tracker.state.value.elapsedMillis)
        assertNull(tracker.state.value.throughput?.downloadBytesPerSecond)
        assertFalse(tracker.state.value.isTunnelUp)
    }

    @Test
    fun theTimerStartsWhenThePlatformSaysTheTunnelIsUp() = fixture {
        bringUp()
        assertTrue(tracker.state.value.isTunnelUp)
        assertEquals(0L, tracker.state.value.elapsedMillis)

        advance(45_000L)

        assertEquals(45_000L, tracker.state.value.elapsedMillis)
    }

    @Test
    fun theTimerAdvancesWhenByteCountersDoNotChange() = fixture(intervalMillis = 1_000L) {
        platform.stats = TunnelStats(totalRx = 1_000L, totalTx = 400L)
        bringUp()
        advance(1_000L)
        advance(1_000L)
        advance(1_000L)

        assertEquals(3_000L, tracker.state.value.elapsedMillis)
    }

    @Test
    fun throughputNeedsTwoRealSamplesOfTheTunnelsOwnCounters() = fixture(intervalMillis = 1_000L) {
        platform.stats = TunnelStats(totalRx = 1_000L, totalTx = 400L)
        bringUp()

        // First sample establishes the base; a rate needs two points.
        advance(1_000L)
        assertNull(tracker.state.value.throughput?.downloadBytesPerSecond)

        // Second sample: 9000 more bytes down and 500 more up in 1000 ms.
        platform.stats = TunnelStats(totalRx = 10_000L, totalTx = 900L)
        advance(1_000L)

        val throughput = tracker.state.value.throughput
        assertNotNull(throughput)
        assertEquals(9_000.0, throughput!!.downloadBytesPerSecond!!, 1e-9)
        assertEquals(500.0, throughput.uploadBytesPerSecond!!, 1e-9)
        assertEquals(1_000L, throughput.windowMillis)
    }

    /**
     * Reconnecting resets the backend's per-tunnel counters. The window straddling that
     * reset must produce no rate rather than a negative one, and the next window must be
     * measured normally from the new base.
     */
    @Test
    fun aCounterResetYieldsNoRateRatherThanANegativeOne() = fixture(intervalMillis = 1_000L) {
        platform.stats = TunnelStats(totalRx = 50_000L, totalTx = 20_000L)
        bringUp()
        advance(1_000L)

        platform.stats = TunnelStats(totalRx = 300L, totalTx = 100L)
        advance(1_000L)
        assertNull(tracker.state.value.throughput?.downloadBytesPerSecond)

        platform.stats = TunnelStats(totalRx = 1_300L, totalTx = 100L)
        advance(1_000L)
        assertEquals(1_000.0, tracker.state.value.throughput!!.downloadBytesPerSecond!!, 1e-9)
    }

    @Test
    fun disconnectingClearsEverySessionNumber() = fixture(intervalMillis = 1_000L) {
        platform.stats = TunnelStats(totalRx = 4_000L, totalTx = 2_000L)
        bringUp()
        advance(1_000L)
        advance(1_000L)
        assertNotNull(tracker.state.value.throughput)

        bringDown()

        val cleared = tracker.state.value
        assertFalse(cleared.isTunnelUp)
        assertNull(cleared.elapsedMillis)
        assertNull(cleared.rxBytes)
        assertNull(cleared.txBytes)
        assertNull(cleared.throughput)
    }

    @Test
    fun theTunnelAddressComesFromTheUsersOwnConfiguration() = fixture {
        assertEquals("10.80.0.2/32", tracker.state.value.tunnelAddress)
        assertEquals("Home gateway", tracker.state.value.endpoint?.displayName)
    }

    @Test
    fun aPingIsAMeasurementOfTheChosenEndpointWithATimestamp() = fixture {
        probe.result = mapOf("e1" to EndpointMeasurement.success(37, System.currentTimeMillis()))

        tracker.pingEndpoint()
        flush()

        assertEquals(37L, tracker.state.value.pingMillis)
        assertNotNull(tracker.state.value.pingMeasuredAtElapsed)
        assertFalse(tracker.state.value.pingFailed)
        assertEquals(1, probe.calls)
    }

    @Test
    fun anUnreachableEndpointIsReportedAsFailedNotAsZeroMillis() = fixture {
        probe.result = mapOf("e1" to EndpointMeasurement.unreachable(System.currentTimeMillis()))

        tracker.pingEndpoint()
        flush()

        assertNull(tracker.state.value.pingMillis)
        assertTrue(tracker.state.value.pingFailed)
    }

    @Test
    fun noPingIsAttemptedWithoutAChosenEndpoint() = fixture(withEndpoint = false) {
        tracker.pingEndpoint()
        flush()

        assertEquals("nothing to measure means no socket is opened", 0, probe.calls)
        assertNull(tracker.state.value.pingMillis)
    }
}
