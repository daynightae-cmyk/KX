package com.kx.vpn.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kx.vpn.domain.servers.EndpointMeasurement
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.servers.ServerCatalogue
import com.kx.vpn.domain.servers.ServerFilter
import com.kx.vpn.domain.servers.ServerRow
import com.kx.vpn.domain.servers.ServerSwitchContext
import com.kx.vpn.domain.servers.ServerSwitchFailure
import com.kx.vpn.domain.servers.ServerSwitchState
import com.kx.vpn.ui.servers.SelectServerScreen
import com.kx.vpn.ui.servers.ServerListState
import com.kx.vpn.ui.theme.KXMotionSpec
import com.kx.vpn.ui.theme.KXTheme
import com.kx.vpn.ui.theme.LocalKXGlass
import com.kx.vpn.ui.theme.LocalKXMotion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the TASK_03 server screen.
 *
 * The point of interest is the honesty of the list: a row shows a latency only when one
 * was measured, the empty state invites an import instead of showing somebody else's
 * servers, and the capability filters are visibly disabled.
 *
 * The clock travels with the rows ([ServerListState.generatedAtMillis]) rather than being
 * read inside the composable, so a render is reproducible: the same fixture always shows
 * the same latency, badge and stale marker.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ServerRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private fun render(dark: Boolean, body: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalKXMotion provides KXMotionSpec.Reduced) {
                KXTheme(darkTheme = dark) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LocalKXGlass.current.background),
                    ) { body() }
                }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val metrics = compose.activity.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val root = compose.activity.window.decorView
        val exactly = android.view.View.MeasureSpec.EXACTLY
        root.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(width, exactly),
            android.view.View.MeasureSpec.makeMeasureSpec(height, exactly),
        )
        root.layout(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))
        val dir = File("build/kx-screenshots").apply { mkdirs() }
        val out = File(dir, "$name.png")
        FileOutputStream(out).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        check(out.length() > 0L) { "empty screenshot written for $name" }
    }

    /** The instant every fixture measurement was taken at. */
    private val now = 1_700_000_000_000L

    private fun row(
        id: String,
        label: String,
        country: String?,
        latency: Long?,
        favorite: Boolean = false,
        measuredAt: Long = now,
    ) = ServerRow(
        profile = EndpointProfile(
            id = id,
            displayName = label,
            countryCode = country,
            host = "203.0.113.$id",
            port = 51820,
            profileId = id,
        ),
        measurement = latency?.let { EndpointMeasurement.success(it, measuredAt) },
        isFavorite = favorite,
        // Derived by the catalogue, exactly as the running app derives it, so a render
        // cannot show a badge the production code would never produce.
        isRecommended = false,
    )

    private fun state(
        rows: List<ServerRow>,
        activeId: String? = null,
        measuredAt: Long = now,
        query: String = "",
        filter: ServerFilter = ServerFilter.All,
    ): ServerListState = ServerListState(
        // Recommendation, search, filtering and ordering all come from the production
        // rules — a render must not be able to show a badge, a row or an order the real
        // catalogue would not produce for the same data.
        rows = produce(rows, measuredAt, query, filter),
        generatedAtMillis = now,
        activeEndpointId = activeId,
    )

    @Composable
    private fun screen(
        state: ServerListState,
        query: String = "",
        selectedFilter: ServerFilter = ServerFilter.All,
        isMeasuring: Boolean = false,
        isOffline: Boolean = false,
        switchState: ServerSwitchState = ServerSwitchState.IDLE,
        switchFailure: ServerSwitchFailure? = null,
    ) = SelectServerScreen(
        state = state,
        query = query,
        selectedFilter = selectedFilter,
        enabledFilters = setOf(ServerFilter.All, ServerFilter.Fastest),
        isMeasuring = isMeasuring,
        isOffline = isOffline,
        switchState = switchState,
        switchFailure = switchFailure,
        onQueryChange = {},
        onFilterChange = {},
        onSelect = {},
        onToggleFavorite = {},
        onDismissSwitchFailure = {},
        onImport = {},
        onBack = {},
    )

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    fun populatedListNight() {
        val rows = listOf(
            row("1", "Frankfurt", "DE", 23),
            row("2", "Amsterdam", "NL", 28),
            row("3", "Singapore", "SG", 36, favorite = true),
            row("4", "Unmeasured Node", null, null),
        )
        render(dark = true) { screen(state = state(rows, activeId = "3")) }
        capture("30-servers-populated-night-411-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun emptyCatalogueDay() {
        render(dark = false) { screen(state = state(emptyList())) }
        capture("31-servers-empty-day-360-en")
    }

    @Test
    @Config(qualifiers = "ar-rEG-w360dp-h640dp-xhdpi")
    fun populatedListArabicRtl() {
        val rows = listOf(
            row("1", "Frankfurt", "DE", 23),
            row("2", "Singapore", "SG", 36, favorite = true),
        )
        render(dark = true) { screen(state = state(rows, activeId = "1")) }
        capture("32-servers-populated-night-360-ar")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun measuringKeepsTheListVisible() {
        // A measurement must not blank the user's own servers: the notice sits above the
        // rows, and the rows keep whatever was last truthfully measured.
        val rows = listOf(
            row("1", "Frankfurt", "DE", 23),
            row("2", "Amsterdam", "NL", 28),
        )
        render(dark = true) {
            screen(
                state = state(
                    rows,
                    activeId = "1",
                    query = "fran",
                    filter = ServerFilter.Fastest,
                ),
                query = "fran",
                selectedFilter = ServerFilter.Fastest,
                isMeasuring = true,
            )
        }
        capture("33-servers-measuring-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun staleMeasurementsAndFailedSwitch() {
        // Ten minutes old: outside the freshness window, so no latency number and no
        // recommendation are displayed even though a measurement exists.
        val stale = now - 10 * 60 * 1000L
        val rows = listOf(
            row("1", "Frankfurt", "DE", 23, measuredAt = stale),
            row("2", "Amsterdam", "NL", 28, measuredAt = stale),
        )
        render(dark = true) {
            screen(
                state = state(rows, activeId = "1", measuredAt = now),
                switchState = ServerSwitchState.FAILED,
                switchFailure = ServerSwitchFailure.ConnectFailed,
            )
        }
        capture("34-servers-stale-failed-switch-night-360-en")
    }

    /**
     * Landscape. The list has to stay reachable rather than pushing the controls off the
     * top of a short viewport.
     */
    @Test
    @Config(qualifiers = "en-rUS-w640dp-h360dp-land-xhdpi")
    fun populatedListLandscape() {
        val rows = listOf(
            row("1", "Frankfurt", "DE", 23),
            row("2", "Amsterdam", "NL", 28),
        )
        render(dark = true) { screen(state = state(rows, activeId = "1")) }
        capture("35-servers-populated-night-640-landscape-en")
    }

    /**
     * No validated network. The screen says so where a spinner would otherwise sit
     * forever, and still shows the servers the user owns.
     */
    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun offlineNotice() {
        val rows = listOf(
            row("1", "Frankfurt", "DE", 23),
            row("2", "Amsterdam", "NL", 28),
        )
        render(dark = false) {
            screen(
                state = state(rows, activeId = "1"),
                isOffline = true,
            )
        }
        capture("36-servers-offline-day-360-en")
    }
}

/**
 * Applies the real catalogue rules to a hand-built list, so the renders show exactly
 * what the production code would show for the same data.
 */
private fun produce(
    rows: List<ServerRow>,
    now: Long,
    query: String,
    filter: ServerFilter,
): List<ServerRow> {
    val measurements = rows.mapNotNull { row -> row.measurement?.let { row.profile.id to it } }.toMap()
    val favorites = rows.filter { it.isFavorite }.map { it.profile.id }.toSet()
    val built = ServerCatalogue.buildRows(
        profiles = rows.map { it.profile },
        measurements = measurements,
        favorites = favorites,
        now = now,
    )
    val searched = ServerCatalogue.search(built, query, countryNames = null)
    return ServerCatalogue.sort(ServerCatalogue.filter(searched, filter, now), now)
}
