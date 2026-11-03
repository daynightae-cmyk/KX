package com.kx.vpn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kx.vpn.data.EndpointCatalogueRepository
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.servers.ServerFilter
import com.kx.vpn.domain.servers.ServerRow
import com.kx.vpn.domain.servers.ServerSwitchState
import com.kx.vpn.ui.servers.SelectServerScreen
import com.kx.vpn.ui.servers.ServerListState
import com.kx.vpn.ui.theme.KXTheme
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device checks for TASK_03.
 *
 * Two things here cannot be proven on the JVM and are the reason this file exists:
 *
 *  * **The catalogue really writes to the platform's file system more than once.** The
 *    JVM behavioural tests run against an in-memory store, because androidx DataStore's
 *    `ATOMIC_MOVE` without `REPLACE_EXISTING` cannot overwrite an existing preferences
 *    file on Windows. On a real Android file system the multi-write path is the shipped
 *    path, so it has to be exercised here rather than assumed.
 *  * **The screen composes on a real renderer** and reports the endpoint it was given
 *     when the user taps it, including that an unmeasured endpoint shows a placeholder
 *     and no fabricated number.
 */
@RunWith(AndroidJUnit4::class)
class ServerCatalogueInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var directory: File

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @After
    fun tearDown() {
        scope.cancel()
        if (::directory.isInitialized) directory.deleteRecursively()
    }

    private fun fileStore(name: String = "catalogue.preferences_pb") =
        PreferenceDataStoreFactory.create(scope = scope) {
            directory = File(context.filesDir, "instrumented-catalogue").apply {
                mkdirs()
            }
            File(directory, name)
        }

    private fun endpoint(id: String, label: String, host: String) = EndpointProfile(
        id = id,
        displayName = label,
        countryCode = null,
        host = host,
        port = 51820,
        profileId = id,
    )

    // -- The real store -----------------------------------------------------------------

    @Test
    fun severalWritesRoundTripThroughTheRealDataStore() = runTest {
        val repository = EndpointCatalogueRepository(fileStore())

        assertTrue(repository.upsert(endpoint("a", "Frankfurt", "203.0.113.1")))
        assertTrue(repository.upsert(endpoint("b", "Tokyo", "198.51.100.9")))
        assertTrue(repository.setActiveEndpoint("b"))

        val state = repository.state.first()
        assertEquals(
            "both endpoints must survive real file writes",
            setOf("Frankfurt", "Tokyo"),
            state.endpoints.map { it.displayName }.toSet(),
        )
        assertEquals("b", state.activeEndpointId)
    }

    @Test
    fun theChosenEndpointSurvivesARepositoryReopen() = runTest {
        val store = fileStore("reopen.preferences_pb")
        EndpointCatalogueRepository(store).apply {
            upsert(endpoint("a", "Frankfurt", "203.0.113.1"))
            upsert(endpoint("b", "Tokyo", "198.51.100.9"))
            setActiveEndpoint("b")
        }

        // A second repository over the same file is what a process restart sees.
        val reopened = EndpointCatalogueRepository(store).state.first()

        assertEquals("b", reopened.activeEndpointId)
        assertEquals(2, reopened.endpoints.size)
    }

    @Test
    fun aChoiceForAnEndpointThatIsNotThereIsRefusedOnTheDevice() = runTest {
        val repository = EndpointCatalogueRepository(fileStore("refuse.preferences_pb"))
        repository.upsert(endpoint("a", "Frankfurt", "203.0.113.1"))

        assertFalse(repository.setActiveEndpoint("does-not-exist"))
        assertEquals("a", repository.state.first().activeEndpointId)
    }

    @Test
    fun removingTheChosenEndpointLeavesTheListUsableOnTheDevice() = runTest {
        val repository = EndpointCatalogueRepository(fileStore("remove.preferences_pb"))
        repository.upsert(endpoint("a", "Frankfurt", "203.0.113.1"))
        repository.upsert(endpoint("b", "Tokyo", "198.51.100.9"))
        repository.setActiveEndpoint("b")

        repository.remove("b")

        val state = repository.state.first()
        assertEquals(listOf("a"), state.endpoints.map { it.id })
        assertEquals("a", state.activeEndpointId)
    }

    // -- The screen on a real renderer --------------------------------------------------

    private fun row(id: String, label: String, latency: Long?) = ServerRow(
        profile = endpoint(id, label, "203.0.113.$id"),
        measurement = latency?.let {
            com.kx.vpn.domain.servers.EndpointMeasurement.success(it, System.currentTimeMillis())
        },
        isFavorite = false,
        isRecommended = false,
    )

    private fun render(
        state: ServerListState,
        onSelect: (ServerRow) -> Unit,
    ) {
        compose.setContent {
            KXTheme {
                SelectServerScreen(
                    state = state,
                    query = "",
                    selectedFilter = ServerFilter.All,
                    enabledFilters = setOf(ServerFilter.All, ServerFilter.Fastest),
                    isMeasuring = false,
                    isOffline = false,
                    switchState = ServerSwitchState.IDLE,
                    switchFailure = null,
                    onQueryChange = {},
                    onFilterChange = {},
                    onSelect = onSelect,
                    onToggleFavorite = {},
                    onDismissSwitchFailure = {},
                    onImport = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun tappingARowOnTheDeviceSelectsThatEndpoint() {
        val rows = listOf(row("a", "Frankfurt", 23), row("b", "Tokyo", 41))
        val selected = mutableListOf<String>()
        render(
            ServerListState(rows = rows, generatedAtMillis = System.currentTimeMillis()),
            onSelect = { selected.add(it.profile.id) },
        )

        compose.onNodeWithText("Tokyo").assertIsDisplayed().performClick()

        assertEquals(listOf("b"), selected)
        // The measured value the device actually rendered is the one that was supplied.
        compose.onNodeWithText("41 ms").assertIsDisplayed()
    }

    @Test
    fun anUnmeasuredEndpointShowsNoNumberOnTheDevice() {
        val rows = listOf(row("a", "Frankfurt", null))
        render(
            ServerListState(rows = rows, generatedAtMillis = System.currentTimeMillis()),
            onSelect = {},
        )

        compose.onNodeWithText("Frankfurt").assertIsDisplayed()
        // Substring matching, so a rendered "23 ms" would actually be found. Without it
        // this assertion would pass even if the screen invented a number.
        val renderedLatencies = compose.onAllNodesWithText(
            text = " ms",
            substring = true,
        ).fetchSemanticsNodes()
        assertTrue(
            "an endpoint that was never measured must not render a latency on a real device either",
            renderedLatencies.isEmpty(),
        )
        compose.onNodeWithText("--").assertIsDisplayed()
    }

    @Test
    fun aDisabledCapabilityFilterIsPresentAndAnnouncedAsDisabled() {
        val rows = listOf(row("a", "Frankfurt", 23))
        render(
            ServerListState(rows = rows, generatedAtMillis = System.currentTimeMillis()),
            onSelect = {},
        )

        // The chip carries its own explanation in its accessibility label, so it is
        // addressed by that rather than by the visible word — the word also appears on a
        // child node, which would make the matcher ambiguous.
        val chipLabel = context.getString(R.string.servers_filter_streaming) + ". " +
            context.getString(R.string.servers_filter_streaming_unavailable)

        // A `clickable(enabled = false)` still publishes an OnClick semantics action —
        // that is how Compose announces "dimmed, not there" to TalkBack — so the
        // meaningful guarantee is the enabled flag, not the absence of the action.
        // Asserting the action was absent is what this test first tried, and the device
        // correctly refused it.
        compose.onNodeWithContentDescription(chipLabel)
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }
}
