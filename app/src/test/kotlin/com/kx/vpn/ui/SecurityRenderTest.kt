package com.kx.vpn.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kx.vpn.data.SecuritySettings
import com.kx.vpn.ui.security.SecurityCenterScreen
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
 * Renders the TASK_05 security centre.
 *
 * Recorded honestly: these frames show what the screen currently claims, which is more
 * than the build can support. The hero card derives "Fully Protected" from the tunnel
 * interface being up alone, and four of the six switches are persisted but change nothing
 * in the configuration the tunnel actually gets. That is the defect these renders exist to
 * make visible rather than to certify.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SecurityRenderTest {

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

    @Composable
    private fun screen(tunnelUp: Boolean, settings: SecuritySettings = SecuritySettings()) =
        SecurityCenterScreen(
            isTunnelUp = tunnelUp,
            settings = settings,
            onToggleKillSwitch = {},
            onToggleDnsLeakProtection = {},
            onToggleIpv6Protection = {},
            onToggleAutoReconnect = {},
            onToggleTrackerBlocking = {},
            onToggleAlwaysOnVpn = {},
            onBack = {},
        )

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    fun securityCenterNightTunnelUp() {
        render(dark = true) { screen(tunnelUp = true) }
        capture("50-security-night-411-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun securityCenterDayTunnelDown() {
        render(dark = false) { screen(tunnelUp = false) }
        capture("51-security-day-360-en-down")
    }

    @Test
    @Config(qualifiers = "ar-rEG-w360dp-h640dp-xhdpi")
    fun securityCenterArabicRtl() {
        render(dark = true) { screen(tunnelUp = true) }
        capture("52-security-night-360-ar")
    }

    /**
     * The switches off. If a control changes nothing when moved, this frame and the one
     * above should be identical apart from the switches themselves — which is what makes
     * the gap visible.
     */
    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    fun securityCenterNightAllSwitchesOff() {
        render(dark = true) {
            screen(
                tunnelUp = true,
                settings = SecuritySettings(
                    killSwitch = false,
                    dnsLeakProtection = false,
                    ipv6Protection = false,
                    autoReconnect = false,
                    trackerBlocking = false,
                    alwaysOnVpn = false,
                ),
            )
        }
        capture("53-security-night-411-en-switches-off")
    }
}
