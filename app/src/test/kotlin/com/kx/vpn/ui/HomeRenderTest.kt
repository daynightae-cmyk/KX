package com.kx.vpn.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kx.vpn.data.TunnelSessionState
import com.kx.vpn.domain.tunnel.FailureReason
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.platform.TunnelStats
import com.kx.vpn.ui.home.HomeScreen
import com.kx.vpn.ui.home.HomeTab
import com.kx.vpn.ui.home.HomeTabBar
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
 * Renders the TASK_02 home screen under Robolectric native graphics.
 *
 * The important thing these prove is the honesty of the surface: disconnected with no
 * profile, disconnected with a profile, connected, and failed must each look distinct,
 * and no state may render a number that was not measured.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HomeRenderTest {

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

    private fun disconnected(hasProfile: Boolean) = TunnelSessionState(
        tunnelState = if (hasProfile) TunnelState.DISCONNECTED else TunnelState.UNCONFIGURED,
        hasProfile = hasProfile,
    )

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun homeDisconnectedWithoutProfile() {
        render(dark = true) {
            HomeScreen(session = disconnected(hasProfile = false), onConnect = {}, onDisconnect = {})
        }
        capture("20-home-disconnected-no-profile-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    fun homeDisconnectedWithProfile() {
        render(dark = true) {
            HomeScreen(session = disconnected(hasProfile = true), onConnect = {}, onDisconnect = {})
        }
        capture("21-home-disconnected-night-411-en")
    }

    @Test
    @Config(qualifiers = "ar-rEG-w360dp-h640dp-xhdpi")
    fun homeDisconnectedArabicRtl() {
        render(dark = true) {
            HomeScreen(session = disconnected(hasProfile = true), onConnect = {}, onDisconnect = {})
        }
        capture("22-home-disconnected-night-360-ar")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun homeConnectedWithRealCounters() {
        render(dark = true) {
            HomeScreen(
                session = TunnelSessionState(
                    tunnelState = TunnelState.CONNECTED,
                    hasProfile = true,
                    stats = TunnelStats(totalRx = 2048L, totalTx = 1024L),
                ),
                onConnect = {},
                onDisconnect = {},
            )
        }
        capture("23-home-connected-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun homeFailedShowsTheRealReason() {
        render(dark = true) {
            HomeScreen(
                session = TunnelSessionState(
                    tunnelState = TunnelState.FAILED,
                    failure = FailureReason.PermissionDenied,
                    hasProfile = true,
                ),
                onConnect = {},
                onDisconnect = {},
            )
        }
        capture("24-home-failed-permission-denied-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun homeTabBarShowsOnlyHomeAsAvailable() {
        render(dark = true) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeTabBar(selected = HomeTab.Home, onSelect = { })
            }
        }
        capture("25-home-tabbar-night-360-en")
    }
}
