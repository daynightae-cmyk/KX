package com.kx.vpn.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.kx.vpn.R
import com.kx.vpn.data.SecuritySettings
import com.kx.vpn.domain.security.SecurityEnforcement
import com.kx.vpn.ui.security.SecurityCenterScreen
import com.kx.vpn.ui.theme.KXMotionSpec
import com.kx.vpn.ui.theme.KXTheme
import com.kx.vpn.ui.theme.LocalKXGlass
import com.kx.vpn.ui.theme.LocalKXMotion
import org.junit.Assert.assertTrue
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
     * The switches off, at a viewport tall enough to show all six rows.
     *
     * This frame was first captured at 891dp and came out **byte-identical** to the
     * switches-on frame: the only control whose stored value changes anything you can see
     * is Always-On VPN, and at that height it sits below the fold, so the scroll view drew
     * the same five rows twice. A duplicate image is not evidence, so the viewport is
     * taller here and the real difference is asserted in the semantics tree below instead
     * of being inferred from pixels.
     */
    @Test
    @Config(qualifiers = "en-rUS-w411dp-h1200dp-xxhdpi")
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
        capture("53-security-night-411-tall-en-switches-off")
    }

    /**
     * The three controls the build does not honour are reported as not enforced whatever
     * their stored value says.
     *
     * Asserted by matching the whole spoken description, because the first version of this
     * test looked up the node by its visible title and read the content description off
     * it — and found nothing. `onNodeWithText` matches the inner `Text`, while the
     * description lives on the card that merges its children. Matching the string the
     * screen actually announces tests the thing that matters and cannot be satisfied by an
     * empty node.
     */
    @Test
    @Config(qualifiers = "en-rUS-w411dp-h1200dp-xxhdpi")
    fun notEnforcedControlsNeverLookEnforced() {
        val badge = compose.activity.getString(R.string.security_badge_not_enforced)
        fun text(res: Int, vararg args: Any) =
            if (args.isEmpty()) compose.activity.getString(res)
            else compose.activity.getString(res, *args)

        render(dark = true) { screen(tunnelUp = true, settings = SecuritySettings()) }

        listOf(
            Triple(R.string.security_kill_switch_title, R.string.security_kill_switch_desc,
                R.string.security_note_kill_switch),
            Triple(R.string.security_ipv6_title, R.string.security_ipv6_desc,
                R.string.security_note_ipv6),
            Triple(R.string.security_auto_reconnect_title, R.string.security_auto_reconnect_desc,
                R.string.security_note_auto_reconnect),
        ).forEach { (titleRes, descRes, noteRes) ->
            val title = text(titleRes)
            val expected = "$title. $badge. ${text(descRes)}. ${text(noteRes)}"
            val matched = compose.onAllNodes(hasContentDescription(expected)).fetchSemanticsNodes()
            assertTrue(
                "\"$title\" must announce \"$expected\"",
                matched.isNotEmpty(),
            )
        }

        // The two controls that do reach the tunnel name the resolver they substitute.
        compose.onNodeWithText(
            text(R.string.security_note_dns_leak, SecurityEnforcement.DNS_LEAK_RESOLVERS),
            substring = true,
        ).assertIsDisplayed()
        compose.onNodeWithText(
            text(R.string.security_note_tracker, SecurityEnforcement.TRACKER_RESOLVERS),
            substring = true,
        ).assertIsDisplayed()
    }
}
