package com.kx.vpn.data

import com.kx.vpn.data.vault.AeadCipher
import com.kx.vpn.data.vault.ProfileBlobStore
import com.kx.vpn.data.vault.ProfileVault
import com.kx.vpn.data.vault.VaultResult
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.servers.ServerSwitchFailure
import com.kx.vpn.domain.servers.ServerSwitchState
import com.kx.vpn.domain.tunnel.FailureReason
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.platform.TunnelStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A configuration that is structurally valid and unique per host, so a test can tell
 * which configuration the backend was actually handed.
 */
private fun configFor(host: String) = """
    [Interface]
    PrivateKey = priv-$host

    [Peer]
    PublicKey = pub-$host
    Endpoint = $host:51820
    AllowedIPs = 0.0.0.0/0
""".trimIndent()

/**
 * Selecting a server has to change what the tunnel uses, in the right order, and must
 * never claim a connection that the platform did not report.
 *
 * The platform here is a recording fake: it does nothing but log what it was asked to do
 * and answer when the test tells it to. That is what makes the ordering assertions
 * meaningful — nothing is up until the test says so.
 */
class EndpointSwitchControllerTest {

    private class FakePlatform : TunnelPlatform {
        val calls = mutableListOf<String>()
        var consentRequired = false
        var up = false

        override fun requestConsent(): ConsentRequest =
            if (consentRequired) ConsentRequest.Required("req") else ConsentRequest.NotRequired

        override fun startTunnel(configText: String) {
            calls += "start($configText)"
            up = true
        }

        override fun stopTunnel() {
            calls += "stop"
            up = false
        }

        override fun isTunnelUp(): Boolean = up

        override fun statistics(): TunnelStats? = null
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

    private class Fixture(platform: FakePlatform) {
        val profiles = ProfileRepository(
            ProfileVault(cipher = ReversingCipher(), store = InMemoryStore()),
        )
        val catalogue = EndpointCatalogueRepository(InMemoryPreferencesDataStore())
        val session = TunnelSessionManager(
            platform = platform,
            profileSource = { profiles.activeConfigText() },
        )

        init {
            profiles.save(PROFILE_A, configFor("203.0.113.1"))
            profiles.save(PROFILE_B, configFor("203.0.113.2"))
        }

        suspend fun seedCatalogue() {
            catalogue.upsert(endpoint(ENDPOINT_A, PROFILE_A, "203.0.113.1"))
            catalogue.upsert(endpoint(ENDPOINT_B, PROFILE_B, "203.0.113.2"))
            // The first upsert became the choice; make the choice explicit and known.
            catalogue.setActiveEndpoint(ENDPOINT_A)
            profiles.activate(PROFILE_A)
            session.refreshProfilePresence()
        }

        companion object {
            fun endpoint(id: String, profileId: String, host: String) = EndpointProfile(
                id = id,
                displayName = id,
                countryCode = null,
                host = host,
                port = 51820,
                profileId = profileId,
            )
        }
    }

    private val endpointB get() = Fixture.endpoint(ENDPOINT_B, PROFILE_B, "203.0.113.2")

    @Test
    fun choosingAServerWhileDisconnectedChangesTheChoiceAndStartsNothing() = runTest {
        val platform = FakePlatform()
        val fixture = Fixture(platform)
        fixture.seedCatalogue()
        val controller = EndpointSwitchController(fixture.session, fixture.profiles, fixture.catalogue, this)
        runCurrent()

        controller.select(endpointB)
        runCurrent()

        assertEquals(ServerSwitchState.IDLE, controller.switch.value.state)
        assertEquals(ENDPOINT_B, fixture.catalogue.state.first().activeEndpointId)
        assertEquals(PROFILE_B, fixture.profiles.activeProfileId())
        assertEquals(configFor("203.0.113.2"), fixture.profiles.activeConfigText())
        // The honest part: nothing was connected, and the session still says so.
        assertTrue(platform.calls.isEmpty())
        assertNotEquals(TunnelState.CONNECTED, fixture.session.state.value.tunnelState)
        assertFalse(fixture.session.state.value.isTunnelUp)
    }

    @Test
    fun choosingAnEndpointWhoseProfileIsMissingFailsAndChangesNothing() = runTest {
        val platform = FakePlatform()
        val fixture = Fixture(platform)
        fixture.seedCatalogue()
        val controller = EndpointSwitchController(fixture.session, fixture.profiles, fixture.catalogue, this)
        runCurrent()

        controller.select(EndpointProfile(
            id = "ghost",
            displayName = "Ghost",
            countryCode = null,
            host = "203.0.113.99",
            port = 51820,
            profileId = "no-such-profile",
        ))
        runCurrent()

        assertEquals(ServerSwitchState.FAILED, controller.switch.value.state)
        assertEquals(ServerSwitchFailure.UnknownEndpoint, controller.switch.value.failure)
        assertEquals(ENDPOINT_A, fixture.catalogue.state.first().activeEndpointId)
        assertEquals(PROFILE_A, fixture.profiles.activeProfileId())
        assertTrue(platform.calls.isEmpty())
    }

    @Test
    fun aLiveSwitchTearsTheOldTunnelDownBeforeApplyingTheNewOne() = runTest {
        val platform = FakePlatform()
        val fixture = Fixture(platform)
        fixture.seedCatalogue()
        val controller = EndpointSwitchController(fixture.session, fixture.profiles, fixture.catalogue, this)
        runCurrent()

        // Bring the tunnel up first, through the session manager, as the app would.
        fixture.session.connect()
        runCurrent()
        fixture.session.onPlatformStateChanged(up = true)
        runCurrent()
        assertEquals(TunnelState.CONNECTED, fixture.session.state.value.tunnelState)
        platform.calls.clear()

        controller.select(endpointB)
        runCurrent()

        // Step 1: only a teardown was requested, and the choice has not moved yet.
        assertEquals(listOf("stop"), platform.calls)
        assertEquals(ServerSwitchState.DISCONNECTING_OLD, controller.switch.value.state)
        assertEquals(ENDPOINT_A, fixture.catalogue.state.first().activeEndpointId)

        // Step 2: the platform confirms the old tunnel is down.
        fixture.session.onPlatformStateChanged(up = false)
        runCurrent()

        assertEquals(ServerSwitchState.CONNECTING, controller.switch.value.state)
        assertEquals(ENDPOINT_B, fixture.catalogue.state.first().activeEndpointId)
        assertEquals(PROFILE_B, fixture.profiles.activeProfileId())
        // The new configuration is the one handed to the backend, and it was handed over
        // only after the stop.
        assertEquals(listOf("stop", "start(${configFor("203.0.113.2")})"), platform.calls)

        // Step 3: the backend reports the new tunnel up. Only then is the switch CONNECTED.
        fixture.session.onPlatformStateChanged(up = true)
        runCurrent()
        assertEquals(ServerSwitchState.CONNECTED, controller.switch.value.state)
        assertEquals(ENDPOINT_B, fixture.catalogue.state.first().activeEndpointId)
    }

    @Test
    fun aFailedLiveSwitchRollsTheChoiceBackAndDoesNotReconnect() = runTest {
        val platform = FakePlatform()
        val fixture = Fixture(platform)
        fixture.seedCatalogue()
        val controller = EndpointSwitchController(fixture.session, fixture.profiles, fixture.catalogue, this)
        runCurrent()

        fixture.session.connect()
        runCurrent()
        fixture.session.onPlatformStateChanged(up = true)
        runCurrent()
        platform.calls.clear()

        controller.select(endpointB)
        runCurrent()
        fixture.session.onPlatformStateChanged(up = false)
        runCurrent()
        assertEquals(ENDPOINT_B, fixture.catalogue.state.first().activeEndpointId)

        // The new endpoint never comes up.
        fixture.session.onBackendFailure(FailureReason.TunnelStartFailed)
        runCurrent()

        assertEquals(ServerSwitchState.IDLE, controller.switch.value.state)
        assertEquals(ServerSwitchFailure.ConnectFailed, controller.switch.value.failure)
        assertEquals(
            "the previous endpoint is the chosen one again",
            ENDPOINT_A,
            fixture.catalogue.state.first().activeEndpointId,
        )
        assertEquals(PROFILE_A, fixture.profiles.activeProfileId())
        assertEquals(
            "a rollback restores the choice, it does not silently reconnect",
            listOf("stop", "start(${configFor("203.0.113.2")})"),
            platform.calls,
        )
        assertFalse(fixture.session.state.value.isTunnelUp)
    }

    @Test
    fun aSwitchIsNotStartedWhileTheTunnelIsStillConnecting() = runTest {
        val platform = FakePlatform()
        platform.consentRequired = true
        val fixture = Fixture(platform)
        fixture.seedCatalogue()
        val controller = EndpointSwitchController(fixture.session, fixture.profiles, fixture.catalogue, this)
        runCurrent()

        fixture.session.connect()
        runCurrent()
        assertEquals(TunnelState.AWAITING_PERMISSION, fixture.session.state.value.tunnelState)

        controller.select(endpointB)
        runCurrent()

        assertEquals(ServerSwitchFailure.Busy, controller.switch.value.failure)
        assertEquals(ENDPOINT_A, fixture.catalogue.state.first().activeEndpointId)
        assertEquals(PROFILE_A, fixture.profiles.activeProfileId())
        assertTrue("nothing was asked of the platform", platform.calls.isEmpty())
    }

    @Test
    fun aDismissedFailureClearsTheBannerState() = runTest {
        val platform = FakePlatform()
        val fixture = Fixture(platform)
        fixture.seedCatalogue()
        val controller = EndpointSwitchController(fixture.session, fixture.profiles, fixture.catalogue, this)
        runCurrent()

        controller.select(
            EndpointProfile("ghost", "Ghost", null, "203.0.113.9", 51820, "missing"),
        )
        runCurrent()
        assertEquals(ServerSwitchState.FAILED, controller.switch.value.state)

        controller.acknowledgeFailure()
        assertEquals(ServerSwitchState.IDLE, controller.switch.value.state)
        assertEquals(null, controller.switch.value.failure)
    }

    @Test
    fun aSecondSelectionWhileASwitchIsInFlightIsIgnored() = runTest {
        val platform = FakePlatform()
        val fixture = Fixture(platform)
        fixture.seedCatalogue()
        val controller = EndpointSwitchController(fixture.session, fixture.profiles, fixture.catalogue, this)
        runCurrent()

        fixture.session.connect()
        runCurrent()
        fixture.session.onPlatformStateChanged(up = true)
        runCurrent()

        controller.select(endpointB)
        runCurrent()
        assertEquals(ServerSwitchState.DISCONNECTING_OLD, controller.switch.value.state)

        // A third endpoint tapped now must not overtake the switch already running.
        controller.select(Fixture.endpoint("e3", "p3", "203.0.113.3"))
        runCurrent()
        assertEquals(ServerSwitchState.DISCONNECTING_OLD, controller.switch.value.state)
        assertEquals(ENDPOINT_A, fixture.catalogue.state.first().activeEndpointId)
    }

    private companion object {
        const val PROFILE_A = "pa"
        const val PROFILE_B = "pb"
        const val ENDPOINT_A = "ea"
        const val ENDPOINT_B = "eb"
    }
}
