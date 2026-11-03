package com.kx.vpn.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kx.vpn.data.ConnectedSessionState
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.session.Throughput
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.ui.connected.ConnectedSessionContent
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
 * Renders the TASK_04 connected screen.
 *
 * The point of interest is which numbers are allowed to exist. The reference artwork shows
 * 92.4 Mbps, 46.8 Mbps, 28 ms, 00:42:18 and a public IP of 91.107.182.204; this screen
 * shows a monotonic timer, rates derived from two real counter samples, a ping measured
 * against the user's own endpoint, and the tunnel address from the user's own
 * configuration — and `--` wherever any of those has not actually been measured.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ConnectedRenderTest {

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

    private val endpoint = EndpointProfile(
        id = "e1",
        displayName = "Frankfurt",
        countryCode = "DE",
        host = "203.0.113.1",
        port = 51820,
        profileId = "p1",
    )

    /** Two samples 2000 ms apart: 11.5 Mbit/s down, 5.7 Mbit/s up. */
    private val measuredThroughput = Throughput(
        downloadBytesPerSecond = 1_437_500.0,
        uploadBytesPerSecond = 718_750.0,
        windowMillis = 2_000L,
        measuredAtElapsedRealtime = 102_000L,
    )

    @Composable
    private fun screen(
        tunnelState: TunnelState,
        session: ConnectedSessionState,
    ) = ConnectedSessionContent(
        tunnelState = tunnelState,
        session = session,
        onDisconnect = {},
        onChangeServer = {},
        onPingAgain = {},
    )

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    fun connectedNightWithRealNumbers() {
        render(dark = true) {
            screen(
                tunnelState = TunnelState.CONNECTED,
                session = ConnectedSessionState(
                    isTunnelUp = true,
                    elapsedMillis = 2_538_000L,
                    rxBytes = 412_000_000L,
                    txBytes = 96_500_000L,
                    throughput = measuredThroughput,
                    pingMillis = 28L,
                    pingMeasuredAtElapsed = 102_000L,
                    endpoint = endpoint,
                    tunnelAddress = "10.80.0.2/32",
                ),
            )
        }
        capture("40-connected-night-411-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun connectedDayBeforeAnythingIsMeasured() {
        // The tunnel is up, so the headline is honest. Nothing else has been measured yet,
        // so every tile must say so rather than showing a plausible zero.
        render(dark = false) {
            screen(
                tunnelState = TunnelState.CONNECTED,
                session = ConnectedSessionState(
                    isTunnelUp = true,
                    elapsedMillis = 1_200L,
                    endpoint = endpoint,
                    tunnelAddress = null,
                ),
            )
        }
        capture("41-connected-day-360-en-unmeasured")
    }

    @Test
    @Config(qualifiers = "ar-rEG-w360dp-h640dp-xhdpi")
    fun connectedArabicRtl() {
        render(dark = true) {
            screen(
                tunnelState = TunnelState.CONNECTED,
                session = ConnectedSessionState(
                    isTunnelUp = true,
                    elapsedMillis = 4_215_000L,
                    rxBytes = 8_400_000L,
                    txBytes = 1_200_000L,
                    throughput = measuredThroughput,
                    pingMillis = 28L,
                    pingMeasuredAtElapsed = 102_000L,
                    endpoint = endpoint,
                    tunnelAddress = "10.80.0.2/32",
                ),
            )
        }
        capture("42-connected-night-360-ar")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun connectingIsNotConnected() {
        // Mid-handshake: the ring must not show the check, and the headline must be the
        // real state rather than the destination it has not reached.
        render(dark = true) {
            screen(
                tunnelState = TunnelState.CONNECTING,
                session = ConnectedSessionState(
                    isTunnelUp = false,
                    endpoint = endpoint,
                ),
            )
        }
        capture("43-connecting-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun failedLeavesNoStaleNumbers() {
        render(dark = true) {
            screen(
                tunnelState = TunnelState.FAILED,
                session = ConnectedSessionState(
                    isTunnelUp = false,
                    endpoint = endpoint,
                ),
            )
        }
        capture("44-failed-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w640dp-h360dp-land-xhdpi")
    fun connectedLandscape() {
        render(dark = true) {
            screen(
                tunnelState = TunnelState.CONNECTED,
                session = ConnectedSessionState(
                    isTunnelUp = true,
                    elapsedMillis = 60_000L,
                    rxBytes = 12_000_000L,
                    txBytes = 3_400_000L,
                    throughput = measuredThroughput,
                    pingMillis = 41L,
                    pingMeasuredAtElapsed = 102_000L,
                    endpoint = endpoint,
                    tunnelAddress = "10.80.0.2/32",
                ),
            )
        }
        capture("45-connected-night-640-landscape-en")
    }
}
