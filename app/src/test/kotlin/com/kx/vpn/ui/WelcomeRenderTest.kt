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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.domain.LaunchSnapshot
import com.kx.vpn.domain.config.BoundedFileReader
import com.kx.vpn.ui.imports.ImportEntryViewModel
import com.kx.vpn.ui.imports.ImportEntryScreen
import com.kx.vpn.ui.onboarding.OnboardingScreen
import com.kx.vpn.ui.onboarding.OnboardingViewModel
import com.kx.vpn.ui.theme.KXMotionSpec
import com.kx.vpn.ui.theme.KXTheme
import com.kx.vpn.ui.theme.LocalKXGlass
import com.kx.vpn.ui.theme.LocalKXMotion
import com.kx.vpn.ui.welcome.WelcomeScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.util.Base64

/**
 * Renders the three TASK_01 screens under Robolectric native graphics and writes PNGs to
 * `app/build/kx-screenshots/`.
 *
 * Same caveat as TASK_00's BrandRenderTest: this proves the composition draws the
 * intended geometry, colour, RTL layout and font scaling through real Skia. It is not a
 * device screenshot and does not discharge G4.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class WelcomeRenderTest {

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

    private fun key(seed: Int): String =
        Base64.getEncoder().encodeToString(ByteArray(32) { (it + seed).toByte() })

    // -- Welcome --------------------------------------------------------------------

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun welcomeNight360() {
        render(dark = true) {
            WelcomeScreen(onGetStarted = {}, onImportConfig = {})
        }
        capture("11-welcome-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    fun welcomeDay411() {
        render(dark = false) {
            WelcomeScreen(onGetStarted = {}, onImportConfig = {})
        }
        capture("12-welcome-day-411-en")
    }

    @Test
    @Config(qualifiers = "ar-rEG-w360dp-h640dp-xhdpi")
    fun welcomeArabicRtlNight() {
        render(dark = true) {
            WelcomeScreen(onGetStarted = {}, onImportConfig = {})
        }
        capture("13-welcome-night-360-ar")
    }

    /**
     * Arabic is the expansion risk: the disclosure body is markedly longer than the
     * English, so this render is the check that the scroll layout absorbs it rather than
     * clipping the controls away.
     */
    @Test
    @Config(qualifiers = "ar-rEG-w360dp-h640dp-xhdpi")
    fun onboardingArabicRtlNight() {
        // Built outside composition: a ViewModel constructed inside a @Composable lambda
        // is recreated on every recomposition, which lint flags and which would silently
        // reset the disclosure state mid-frame.
        val viewModel = OnboardingViewModel(StubRepository())
        render(dark = true) {
            OnboardingScreen(viewModel = viewModel, onCompleted = {})
        }
        capture("14-onboarding-night-360-ar")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun onboardingNight360ContinueLocked() {
        val viewModel = OnboardingViewModel(StubRepository())
        render(dark = true) {
            OnboardingScreen(viewModel = viewModel, onCompleted = {})
        }
        capture("15-onboarding-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun importIdleNight360() {
        val viewModel = ImportEntryViewModel()
        render(dark = true) {
            ImportEntryScreen(viewModel = viewModel)
        }
        capture("16-import-idle-night-360-en")
    }

    /** A rejected hostile file: the UI must show findings, not a generic failure. */
    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun importRejectedNight360() {
        val viewModel = ImportEntryViewModel()
        viewModel.onBytesRead(
            BoundedFileReader.Outcome.Read(
                """
                    [Interface]
                    PrivateKey = not-a-real-key
                    Address = 999.1.1.1/32

                    [Peer]
                    PublicKey = ${key(9).take(20)}
                """.trimIndent().toByteArray(),
            ),
        )
        render(dark = true) {
            ImportEntryScreen(viewModel = viewModel)
        }
        capture("17-import-rejected-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun importInspectedNight360() {
        val viewModel = ImportEntryViewModel()
        viewModel.onBytesRead(
            BoundedFileReader.Outcome.Read(
                """
                    [Interface]
                    PrivateKey = ${key(1)}
                    Address = 10.8.0.2/32
                    DNS = 1.1.1.1

                    [Peer]
                    PublicKey = ${key(2)}
                    Endpoint = 203.0.113.7:51820
                    AllowedIPs = 0.0.0.0/0
                    PersistentKeepalive = 25
                """.trimIndent().toByteArray(),
            ),
        )
        render(dark = true) {
            ImportEntryScreen(viewModel = viewModel)
        }
        capture("18-import-inspected-night-360-en")
    }

    @Test
    @Config(qualifiers = "ar-rEG-w411dp-h891dp-xxhdpi")
    fun importInspectedArabicDay411() {
        val viewModel = ImportEntryViewModel()
        viewModel.onBytesRead(
            BoundedFileReader.Outcome.Read(
                """
                    [Interface]
                    PrivateKey = ${key(1)}
                    Address = 10.8.0.2/32

                    [Peer]
                    PublicKey = ${key(2)}
                    Endpoint = wg.example.com:443
                    AllowedIPs = 10.0.0.0/8, 192.168.0.0/16
                """.trimIndent().toByteArray(),
            ),
        )
        render(dark = false) {
            ImportEntryScreen(viewModel = viewModel)
        }
        capture("19-import-inspected-day-411-ar")
    }

    /**
     * Guards the claim the import screen makes in its own footnote. If a private key ever
     * became renderable through the summary path, this fails.
     */
    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun privateKeyNeverReachesTheRenderedTree() {
        val secret = key(1)
        val viewModel = ImportEntryViewModel()
        viewModel.onBytesRead(
            BoundedFileReader.Outcome.Read(
                """
                    [Interface]
                    PrivateKey = $secret
                    Address = 10.8.0.2/32

                    [Peer]
                    PublicKey = ${key(2)}
                    AllowedIPs = 0.0.0.0/0
                    Endpoint = 203.0.113.7:51820
                """.trimIndent().toByteArray(),
            ),
        )
        render(dark = true) {
            ImportEntryScreen(viewModel = viewModel)
        }
        compose.waitForIdle()
        val tree = compose.onRoot(useUnmergedTree = true).printToString()
        check(!tree.contains(secret)) { "private key was rendered into the semantics tree" }

        val hostile = ImportEntryViewModel()
        val badKey = key(7)
        hostile.onBytesRead(
            BoundedFileReader.Outcome.Read(
                "[Interface]\nPrivateKey = $badKey\nAddress = bad\n".toByteArray(),
            ),
        )
        val errorTree = hostile.state.value.toString()
        check(!errorTree.contains(badKey)) { "private key leaked into the rejection state" }
    }

    private class StubRepository : LaunchStateRepository {
        override val snapshot: Flow<LaunchSnapshot> = MutableStateFlow(LaunchSnapshot())
        override suspend fun completeOnboarding() = Unit
    }
}
