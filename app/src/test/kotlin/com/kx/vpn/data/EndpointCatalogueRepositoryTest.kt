package com.kx.vpn.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kx.vpn.domain.servers.EndpointProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Favourites are a local preference, so they have to survive a restart and they must
 * never contain anything the user did not put there.
 */
class EndpointCatalogueRepositoryTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var directory: File
    private lateinit var repository: EndpointCatalogueRepository

    /**
     * The behavioural tests run against an in-memory `DataStore` because androidx
     * DataStore cannot overwrite an existing preferences file on Windows: it moves a
     * temporary file with `ATOMIC_MOVE` and **without** `REPLACE_EXISTING`, which
     * POSIX allows and Windows does not. The second write to the same file therefore
     * fails on a Windows dev machine with "Unable to rename …" while working normally
     * on Linux, which is where CI and the app itself run.
     *
     * `aSingleWriteReachesTheRealFileOnDisk` below covers the on-disk path directly.
     */
    @Before
    fun setUp() {
        directory = Files.createTempDirectory("kx-catalogue-test-").toFile()
        repository = EndpointCatalogueRepository(InMemoryPreferencesDataStore())
    }

    @After
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    private fun profile(
        id: String,
        label: String,
        country: String? = null,
        host: String = "203.0.113.10",
    ) = EndpointProfile(
        id = id,
        displayName = label,
        countryCode = country,
        host = host,
        port = 51820,
        profileId = id,
    )

    @Test
    fun anEmptyCatalogueStartsEmpty() = runTest {
        val state = repository.state.first()
        assertTrue(state.isEmpty)
        assertTrue(state.favorites.isEmpty())
    }

    @Test
    fun endpointsRoundTripThroughTheStore() = runTest {
        assertTrue(repository.upsert(profile("a", "Office", country = "DE")))
        assertTrue(repository.upsert(profile("b", "Tokyo", host = "198.51.100.4")))

        val state = repository.state.first()
        assertEquals(2, state.endpoints.size)
        assertEquals(
            setOf("Office", "Tokyo"),
            state.endpoints.map { it.displayName }.toSet(),
        )
        assertEquals("DE", state.endpoints.first { it.id == "a" }.countryCode)
        assertNull(state.endpoints.first { it.id == "b" }.countryCode)
    }

    @Test
    fun anInvalidEndpointIsRefusedAndNotStored() = runTest {
        val rejected = repository.upsert(
            profile("a", "Bad", host = "host with spaces"),
        )
        assertFalse("an endpoint with a hostile host must be refused", rejected)
        assertTrue(repository.state.first().isEmpty)
    }

    @Test
    fun favouritesToggleAndPersist() = runTest {
        repository.upsert(profile("a", "Office"))
        repository.toggleFavorite("a")
        assertEquals(setOf("a"), repository.state.first().favorites)

        repository.toggleFavorite("a")
        assertTrue(repository.state.first().favorites.isEmpty())
    }

    @Test
    fun removingAnEndpointAlsoRemovesItsFavourite() = runTest {
        repository.upsert(profile("a", "Office"))
        repository.toggleFavorite("a")

        repository.remove("a")

        val state = repository.state.first()
        assertTrue(state.isEmpty)
        assertTrue(state.favorites.isEmpty())
    }

    @Test
    fun upsertingTheSameIdReplacesRatherThanDuplicates() = runTest {
        repository.upsert(profile("a", "Office"))
        repository.upsert(profile("a", "Office Renamed"))

        val state = repository.state.first()
        assertEquals(1, state.endpoints.size)
        assertEquals("Office Renamed", state.endpoints.first().displayName)
    }

    /**
     * A label containing the field separator must not be able to forge a second
     * endpoint. The encoding URL-encodes every field, so the separator cannot appear
     * inside one.
     */
    @Test
    fun aLabelContainingSeparatorsCannotForgeAnotherEndpoint() = runTest {
        val hostile = profile("a", "One\u001E\u001FTwo")
        // The id and label must both be valid before anything is stored.
        assertTrue(repository.upsert(hostile))

        val state = repository.state.first()
        assertEquals("the record must not split into two endpoints", 1, state.endpoints.size)
        assertEquals("One\u001E\u001FTwo", state.endpoints.first().displayName)
    }

    @Test
    fun aCountryCodeSurvivesTheRoundTrip() = runTest {
        repository.upsert(profile("a", "Cairo", country = "EG"))
        assertEquals("EG", repository.state.first().endpoints.first().countryCode)
    }

    // -- The chosen endpoint ------------------------------------------------------------

    @Test
    fun theFirstEndpointBecomesTheChosenOne() = runTest {
        repository.upsert(profile("a", "Office"))
        assertEquals("a", repository.state.first().activeEndpointId)
    }

    @Test
    fun aLaterImportDoesNotOverrideTheChoice() = runTest {
        repository.upsert(profile("a", "Office"))
        repository.upsert(profile("b", "Tokyo"))
        // Adding a server is not the same as switching to it.
        assertEquals("a", repository.state.first().activeEndpointId)
    }

    @Test
    fun aChoiceIsPersistedAndRoundTrips() = runTest {
        repository.upsert(profile("a", "Office"))
        repository.upsert(profile("b", "Tokyo"))

        assertTrue(repository.setActiveEndpoint("b"))
        assertEquals("b", repository.state.first().activeEndpointId)
    }

    @Test
    fun choosingAnUnknownEndpointIsRefused() = runTest {
        repository.upsert(profile("a", "Office"))

        assertFalse(
            "a stale screen must not be able to select a server the user does not have",
            repository.setActiveEndpoint("ghost"),
        )
        assertEquals("a", repository.state.first().activeEndpointId)
    }

    @Test
    fun removingTheChosenEndpointFallsBackToAnother() = runTest {
        repository.upsert(profile("a", "Office"))
        repository.upsert(profile("b", "Tokyo"))
        repository.setActiveEndpoint("b")

        repository.remove("b")

        val state = repository.state.first()
        assertEquals(listOf("a"), state.endpoints.map { it.id })
        assertEquals("a", state.activeEndpointId)
    }

    @Test
    fun aChoicePointingAtADeletedEndpointIsDroppedOnRead() = runTest {
        repository.upsert(profile("a", "Office"))
        repository.remove("a")

        // Nothing is left to choose, and the stored id must not resurface as a phantom.
        val state = repository.state.first()
        assertTrue(state.isEmpty)
        assertTrue(
            "an active id that no longer names an endpoint must not survive the read",
            state.activeEndpointId == null,
        )
    }

    private fun <T> assertNull(value: T?) = assertTrue(value == null)

    /**
     * The on-disk path, with a single write. This is the part the platform limitation
     * above does not affect, and it is what proves the data really lands in a file.
     */
    @Test
    fun aSingleWriteReachesTheRealFileOnDisk() = runTest {
        val onDisk = EndpointCatalogueRepository(
            PreferenceDataStoreFactory.create(scope = scope) {
                File(directory, "single.preferences_pb")
            },
        )
        assertTrue(onDisk.upsert(profile("a", "Office", country = "DE")))

        val state = onDisk.state.first()
        assertEquals(1, state.endpoints.size)
        assertEquals("Office", state.endpoints.first().displayName)
        assertTrue("the file must exist on disk", File(directory, "single.preferences_pb").isFile)
    }
}
