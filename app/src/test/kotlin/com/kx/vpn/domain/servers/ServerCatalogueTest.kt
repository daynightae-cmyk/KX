package com.kx.vpn.domain.servers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The rules that decide what the server list shows and in what order.
 *
 * The property that matters most is honesty: a latency is only ever shown when it was
 * really measured and is still fresh, and an endpoint only exists because the user
 * configured it.
 */
class ServerCatalogueTest {

    private val now = 1_700_000_000_000L

    private fun profile(
        id: String,
        label: String,
        host: String = "203.0.113.$id",
        country: String? = null,
    ) = EndpointProfile(
        id = id,
        displayName = label,
        countryCode = country,
        host = host,
        port = 51820,
        profileId = id,
    )

    private fun measurement(latency: Long, at: Long = now) =
        EndpointMeasurement.success(latency, at)

    // -- Rows and recommendation --------------------------------------------------------

    @Test
    fun theFastestMeasuredEndpointIsRecommended() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Alpha"), profile("2", "Beta"), profile("3", "Gamma")),
            measurements = mapOf(
                "1" to measurement(90),
                "2" to measurement(30),
                "3" to measurement(60),
            ),
            favorites = emptySet(),
            now = now,
        )
        assertEquals("2", rows.single { it.isRecommended }.profile.id)
        assertEquals(1, rows.count { it.isRecommended })
    }

    @Test
    fun nothingIsRecommendedWhenNothingWasMeasured() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Alpha")),
            measurements = emptyMap(),
            favorites = emptySet(),
            now = now,
        )
        assertTrue("an unmeasured endpoint is not a recommendation", rows.none { it.isRecommended })
    }

    @Test
    fun aStaleMeasurementDoesNotEarnARecommendation() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Alpha"), profile("2", "Beta")),
            measurements = mapOf(
                "1" to measurement(10, at = now - EndpointMeasurement.STALE_AFTER_MILLIS - 1),
                "2" to measurement(200),
            ),
            favorites = emptySet(),
            now = now,
        )
        assertEquals("2", rows.single { it.isRecommended }.profile.id)
    }

    // -- Displayed latency ----------------------------------------------------------------

    @Test
    fun onlyFreshReachableMeasurementsAreDisplayed() {
        val fresh = EndpointMeasurement.success(42, now)
        val stale = EndpointMeasurement.success(42, now - EndpointMeasurement.STALE_AFTER_MILLIS - 1)
        val unreachable = EndpointMeasurement.unreachable(now)

        assertEquals(42L, fresh.displayLatency(now))
        assertNull("a stale measurement must not be shown as current", stale.displayLatency(now))
        assertNull("an unreachable endpoint has no latency to show", unreachable.displayLatency(now))
    }

    @Test
    fun anUnreachableEndpointHasNoLatencyAtAll() {
        val measurement = EndpointMeasurement.unreachable(now)
        assertFalse(measurement.reachable)
        assertNull(measurement.latencyMillis)
        assertNull(measurement.displayLatency(now))
    }

    // -- Sorting ---------------------------------------------------------------------------

    @Test
    fun sortingPutsFavouritesFirstThenMeasuredThenUnmeasured() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Alpha"), profile("2", "Beta"), profile("3", "Gamma")),
            measurements = mapOf(
                "1" to measurement(50),
                "2" to measurement(10),
            ),
            favorites = setOf("3"),
            now = now,
        )
        val sorted = ServerCatalogue.sort(rows, now)
        assertEquals("3", sorted[0].profile.id)
        assertEquals("2", sorted[1].profile.id)
        assertEquals("1", sorted[2].profile.id)
    }

    @Test
    fun sortingIsStableForEqualLatencies() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("b", "Bravo"), profile("a", "Alpha")),
            measurements = mapOf("a" to measurement(20), "b" to measurement(20)),
            favorites = emptySet(),
            now = now,
        )
        val sorted = ServerCatalogue.sort(rows, now)
        assertEquals(listOf("Alpha", "Bravo"), sorted.map { it.profile.displayName })
    }

    // -- Filtering -------------------------------------------------------------------------

    @Test
    fun theFastestFilterKeepsOnlyMeasuredEndpointsAndLimitsTheCount() {
        val profiles = (1..5).map { profile("$it", "Node $it") }
        val rows = ServerCatalogue.buildRows(
            profiles = profiles,
            measurements = profiles.associate { it.id to measurement(it.id.toLong() * 10) },
            favorites = emptySet(),
            now = now,
        )
        val fastest = ServerCatalogue.filter(rows, ServerFilter.Fastest, now)
        assertEquals(ServerCatalogue.FASTEST_LIMIT, fastest.size)
        assertEquals(listOf(10L, 20L, 30L), fastest.map { it.measurement?.latencyMillis })
    }

    @Test
    fun theFastestFilterExcludesUnmeasuredEndpoints() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Alpha"), profile("2", "Beta")),
            measurements = mapOf("1" to measurement(20)),
            favorites = emptySet(),
            now = now,
        )
        val fastest = ServerCatalogue.filter(rows, ServerFilter.Fastest, now)
        assertEquals(listOf("1"), fastest.map { it.profile.id })
    }

    /**
     * A measurement that has gone stale cannot be displayed as a latency, so it must not
     * survive the "Fastest" filter either. Otherwise the filter would show a row that
     * contradicts itself: selected as fastest, with no number to justify it.
     */
    @Test
    fun theFastestFilterExcludesStaleMeasurements() {
        val staleAt = now - EndpointMeasurement.STALE_AFTER_MILLIS - 1
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Alpha"), profile("2", "Beta")),
            measurements = mapOf(
                "1" to measurement(20, at = staleAt),
                "2" to measurement(80),
            ),
            favorites = emptySet(),
            now = now,
        )
        val fastest = ServerCatalogue.filter(rows, ServerFilter.Fastest, now)
        assertEquals(
            "only the fresh measurement may be offered as fastest",
            listOf("2"),
            fastest.map { it.profile.id },
        )
    }

    /**
     * A filter that needs capability metadata nobody supplied must not silently empty
     * the list. It is disabled in the UI, and if it is applied anyway it is a no-op.
     */
    @Test
    fun capabilityFiltersAreDisabledWithoutMetadata() {
        assertTrue(ServerFilter.All.isEnabled(hasCapability = false))
        assertTrue(ServerFilter.Fastest.isEnabled(hasCapability = false))
        assertFalse(ServerFilter.Streaming.isEnabled(hasCapability = false))
        assertFalse(ServerFilter.Gaming.isEnabled(hasCapability = false))
        assertTrue(ServerFilter.Streaming.isEnabled(hasCapability = true))
    }

    @Test
    fun applyingADisabledFilterLeavesTheListIntact() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Alpha")),
            measurements = emptyMap(),
            favorites = emptySet(),
            now = now,
        )
        assertEquals(rows, ServerCatalogue.filter(rows, ServerFilter.Streaming, now))
        assertEquals(rows, ServerCatalogue.filter(rows, ServerFilter.Gaming, now))
    }

    // -- Search ---------------------------------------------------------------------------

    @Test
    fun searchMatchesTheUsersOwnLabel() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Home Office"), profile("2", "Tokyo")),
            measurements = emptyMap(),
            favorites = emptySet(),
            now = now,
        )
        assertEquals(
            listOf("Home Office"),
            ServerCatalogue.search(rows, "home", null).map { it.profile.displayName },
        )
    }

    @Test
    fun searchIsDiacriticInsensitive() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "São Paulo")),
            measurements = emptyMap(),
            favorites = emptySet(),
            now = now,
        )
        assertEquals(1, ServerCatalogue.search(rows, "sao paulo", null).size)
        assertEquals(1, ServerCatalogue.search(rows, "SÃO PAULO", null).size)
    }

    @Test
    fun searchMatchesAnArabicCountryNameWhenTheResolverProvidesIt() {
        val resolver = FakeCountryNames(
            mapOf("EG" to mapOf("en" to "Egypt", "ar" to "مصر")),
        )
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Cairo Node", country = "EG")),
            measurements = emptyMap(),
            favorites = emptySet(),
            now = now,
        )
        assertEquals(1, ServerCatalogue.search(rows, "مصر", resolver, Locale("ar")).size)
        assertEquals(1, ServerCatalogue.search(rows, "egypt", resolver, Locale("ar")).size)
    }

    @Test
    fun searchMatchesTheCountryCode() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Node", country = "DE")),
            measurements = emptyMap(),
            favorites = emptySet(),
            now = now,
        )
        assertEquals(1, ServerCatalogue.search(rows, "DE", null).size)
    }

    @Test
    fun anEmptyQueryReturnsEverything() {
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Alpha"), profile("2", "Beta")),
            measurements = emptyMap(),
            favorites = emptySet(),
            now = now,
        )
        assertEquals(2, ServerCatalogue.search(rows, "   ", null).size)
    }

    @Test
    fun searchNeverMatchesAPartialWordInTheMiddleOfTheHost() {
        // A host is searchable, but only as a whole folded substring; a search must
        // not be a way to enumerate other people's infrastructure.
        val rows = ServerCatalogue.buildRows(
            profiles = listOf(profile("1", "Node", host = "vpn.example.com")),
            measurements = emptyMap(),
            favorites = emptySet(),
            now = now,
        )
        assertEquals(1, ServerCatalogue.search(rows, "example", null).size)
        assertEquals(0, ServerCatalogue.search(rows, "example.internal", null).size)
    }

    // -- Host validation -------------------------------------------------------------------

    @Test
    fun validHostsAreAccepted() {
        listOf(
            "vpn.example.com",
            "wg.example.co.uk",
            "203.0.113.7",
            "198.51.100.2",
            "2001:db8::1",
        ).forEach { host ->
            assertTrue("$host should be valid", EndpointValidator.isValidHost(host))
        }
    }

    @Test
    fun hostileOrMalformedHostsAreRejected() {
        listOf(
            "example.com; rm -rf /",
            "example.com/../etc/passwd",
            "host with spaces",
            "http://example.com",
            "example.com:51820",
            "-leading-dash.com",
            "example..com",
            "",
            "a".repeat(300) + ".com",
        ).forEach { host ->
            assertFalse("$host should be rejected", EndpointValidator.isValidHost(host))
        }
    }

    @Test
    fun aHostWithNewlinesIsRejected() {
        assertFalse(EndpointValidator.isValidHost("example.com\nattacker.example"))
        assertFalse(EndpointValidator.isValidHost("example.com\rattacker.example"))
    }

    @Test
    fun validationRejectsAnEndpointWithAnyProblem() {
        val problems = EndpointValidator.validate(
            EndpointProfile(
                id = "ok",
                displayName = "",
                countryCode = "TOOLONG",
                host = "bad host",
                port = 0,
                profileId = "ok",
            ),
        )
        assertTrue(problems.contains(EndpointRejection.EmptyLabel))
        assertTrue(problems.contains(EndpointRejection.InvalidHost))
        assertTrue(problems.contains(EndpointRejection.InvalidPort))
        assertTrue(problems.contains(EndpointRejection.InvalidCountryCode))
    }

    @Test
    fun aValidProfileHasNoProblems() {
        val problems = EndpointValidator.validate(
            EndpointProfile(
                id = "ok",
                displayName = "Office",
                countryCode = "DE",
                host = "vpn.example.com",
                port = 51820,
                profileId = "ok",
            ),
        )
        assertTrue(problems.toString(), problems.isEmpty())
    }

    @Test
    fun aCountryCodeProducesAFlagAndItsAbsenceProducesNone() {
        assertEquals("🇩🇪", profile("1", "Berlin", country = "DE").flag)
        assertNull(profile("1", "Berlin").flag)
        assertNull(profile("1", "Berlin", country = "12").flag)
    }

    // -- Reading the endpoint out of a configuration ----------------------------------------

    @Test
    fun theEndpointIsReadFromTheUsersOwnConfiguration() {
        val config = """
            [Interface]
            PrivateKey = abc
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = def
            AllowedIPs = 0.0.0.0/0
            Endpoint = 203.0.113.7:51820
        """.trimIndent()
        val endpoint = ProfileEndpointReader.read(config)
        assertNotNull(endpoint)
        assertEquals("203.0.113.7", endpoint!!.host)
        assertEquals(51820, endpoint.port)
    }

    /**
     * The peer key is what makes two configurations against the same host different
     * servers, so the reader has to keep it and the id has to depend on it.
     */
    @Test
    fun thePeerPublicKeyIsReadFromTheConfiguration() {
        val config = """
            [Interface]
            PrivateKey = abc

            [Peer]
            PublicKey = pubkeyAAA
            Endpoint = 203.0.113.7:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        assertEquals("pubkeyAAA", ProfileEndpointReader.read(config)?.publicKey)
    }

    @Test
    fun onlyTheFirstPeerOfAMultiPeerConfigurationDescribesTheEntry() {
        val config = """
            [Interface]
            PrivateKey = abc

            [Peer]
            PublicKey = first
            Endpoint = 203.0.113.1:51820
            AllowedIPs = 0.0.0.0/0

            [Peer]
            PublicKey = second
            Endpoint = 203.0.113.2:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        val endpoint = ProfileEndpointReader.read(config)
        assertEquals("203.0.113.1", endpoint?.host)
        assertEquals("first", endpoint?.publicKey)
    }

    @Test
    fun theInterfaceBlockCannotSupplyAPeerKey() {
        val config = """
            [Interface]
            PrivateKey = abc
            PublicKey = not-a-peer-key

            [Peer]
            Endpoint = 203.0.113.7:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        val endpoint = ProfileEndpointReader.read(config)
        assertEquals("203.0.113.7", endpoint?.host)
        assertNull("an Interface key must not be read as the peer identity", endpoint?.publicKey)
    }

    // -- Profile identity -----------------------------------------------------------------

    @Test
    fun theSameServerAlwaysYieldsTheSameProfileId() {
        val first = EndpointIdentity.profileId("203.0.113.7", 51820, "pubkeyAAA")
        val second = EndpointIdentity.profileId("203.0.113.7", 51820, "pubkeyAAA")
        assertEquals(first, second)
        assertTrue(first.matches(Regex("^[A-Za-z0-9_-]{1,64}$")))
    }

    @Test
    fun aDifferentPeerKeyIsADifferentProfile() {
        assertNotEquals(
            "two accounts on one host are not the same server",
            EndpointIdentity.profileId("203.0.113.7", 51820, "pubkeyAAA"),
            EndpointIdentity.profileId("203.0.113.7", 51820, "pubkeyBBB"),
        )
        assertNotEquals(
            EndpointIdentity.profileId("203.0.113.7", 51820, "pubkeyAAA"),
            EndpointIdentity.profileId("203.0.113.8", 51820, "pubkeyAAA"),
        )
    }

    @Test
    fun aConfigurationWithoutAnEndpointYieldsNothing() {
        val config = """
            [Interface]
            PrivateKey = abc
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = def
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        assertNull(ProfileEndpointReader.read(config))
    }

    @Test
    fun aHostileEndpointInAConfigurationIsNotAccepted() {
        val config = """
            [Peer]
            PublicKey = def
            AllowedIPs = 0.0.0.0/0
            Endpoint = bad host; rm -rf /:51820
        """.trimIndent()
        assertNull(ProfileEndpointReader.read(config))
    }

    private class FakeCountryNames(
        private val names: Map<String, Map<String, String>>,
    ) : CountryNameResolver {
        override fun displayName(countryCode: String, locale: Locale): String =
            names[countryCode.uppercase()]?.get(locale.language) ?: countryCode
    }
}
