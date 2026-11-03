package com.kx.vpn.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.kx.vpn.R
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.domain.LaunchSnapshot
import com.kx.vpn.ui.brand.GlassCard
import com.kx.vpn.ui.brand.IconAction
import com.kx.vpn.ui.brand.KXButton
import com.kx.vpn.ui.brand.KXButtonStyle
import com.kx.vpn.ui.brand.KXOrb
import com.kx.vpn.ui.launch.LaunchScreen
import com.kx.vpn.ui.launch.LaunchViewModel
import com.kx.vpn.ui.placeholder.PlaceholderScreen
import com.kx.vpn.ui.theme.KXDimens
import com.kx.vpn.ui.theme.KXMotionSpec
import com.kx.vpn.ui.theme.KXTheme
import com.kx.vpn.ui.theme.LocalKXGlass
import com.kx.vpn.ui.theme.LocalKXMotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the real Compose tree under Robolectric's native graphics backend and writes
 * PNGs to `app/build/kx-screenshots/`.
 *
 * This exists because the quality gates demand pixel evidence and no accelerated
 * emulator is available on this machine. It is *not* a substitute for device testing:
 * it proves the composition draws what we expect, not that it survives a real GPU,
 * real insets or a real activity lifecycle. That gap is recorded as DEVICE_UNVERIFIED
 * in docs/BLOCKERS.md.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BrandRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    /**
     * Reduced motion is used deliberately: an infinite orbital transition never
     * reaches idle, which would make the capture non-deterministic frame to frame.
     */
    private fun render(dark: Boolean, body: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalKXMotion provides KXMotionSpec.Reduced) {
                KXTheme(darkTheme = dark) {
                    // Paint the theme surface so the captured frame shows the actual
                    // day/night backdrop rather than the window default.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LocalKXGlass.current.background),
                    ) { body() }
                }
            }
        }
    }

    /**
     * Draws the live view hierarchy into a [Bitmap] instead of using
     * `captureToImage()`.
     *
     * `captureToImage` asks the window to perform a forced redraw and waits for the
     * frame callback. Under Robolectric that callback never arrives, so every capture
     * times out after 2 seconds. Driving measure/layout/draw ourselves exercises the
     * same Compose render pipeline through the same native Skia backend and produces a
     * real PNG.
     */
    private fun capture(name: String) {
        compose.waitForIdle()
        val metrics = compose.activity.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val root = compose.activity.window.decorView
        val specExactly = android.view.View.MeasureSpec.EXACTLY
        root.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(width, specExactly),
            android.view.View.MeasureSpec.makeMeasureSpec(height, specExactly),
        )
        root.layout(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bitmap))

        val dir = File("build/kx-screenshots").apply { mkdirs() }
        val out = File(dir, "$name.png")
        FileOutputStream(out).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "PNG compression failed for $name"
            }
        }
        check(out.length() > 0L) { "empty screenshot written for $name" }
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun orbNight360() {
        render(dark = true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KXOrb(size = KXDimens.OrbHero)
            }
        }
        capture("01-orb-night-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun orbDay360() {
        render(dark = false) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KXOrb(size = KXDimens.OrbHero)
            }
        }
        capture("02-orb-day-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    fun orbNight411() {
        render(dark = true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KXOrb(size = KXDimens.OrbHero)
            }
        }
        capture("03-orb-night-411-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun orbInlineSmallSize() {
        render(dark = true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KXOrb(size = KXDimens.OrbInline, showWordmark = false)
            }
        }
        capture("10-orb-inline-40dp-night")
    }

    /**
     * Arabic rendering reads the real `values-ar` bundle through the resource system
     * rather than pasting literals, so this test fails if the AR resources are missing
     * or if RTL layout breaks the screen.
     */
    @Test
    @Config(qualifiers = "ar-rEG-w360dp-h640dp-xhdpi")
    fun placeholderArabicRtlNight() {
        render(dark = true) {
            PlaceholderScreen(
                title = stringResource(R.string.home_title),
                body = stringResource(R.string.home_body),
                actionLabel = stringResource(R.string.home_action),
                actionDisabledReason = stringResource(R.string.home_action_unavailable),
                storageDegraded = false,
            )
        }
        capture("04-placeholder-night-360-ar")
    }

    @Test
    @Config(qualifiers = "ar-rEG-w411dp-h891dp-xxhdpi")
    fun placeholderArabicRtlDay() {
        render(dark = false) {
            PlaceholderScreen(
                title = stringResource(R.string.home_title),
                body = stringResource(R.string.home_body),
                actionLabel = stringResource(R.string.home_action),
                actionDisabledReason = stringResource(R.string.home_action_unavailable),
                storageDegraded = false,
            )
        }
        capture("05-placeholder-day-411-ar")
    }

    /**
     * Large-font rendering. Done by overriding [LocalDensity] rather than a Robolectric
     * qualifier string, because the qualifier form for font scale is not stable across
     * Robolectric versions and silently failing to apply 1.3x would make this test
     * prove nothing.
     */
    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun placeholderLargeFontWithDegradedStore() {
        compose.setContent {
            val base = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                LocalKXMotion provides KXMotionSpec.Reduced,
                androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit.Density(
                        density = base.density,
                        fontScale = 1.3f,
                    ),
            ) {
                KXTheme(darkTheme = true) {
                    PlaceholderScreen(
                        title = stringResource(R.string.home_title),
                        body = stringResource(R.string.home_body),
                        actionLabel = stringResource(R.string.home_action),
                        actionDisabledReason = stringResource(R.string.home_action_unavailable),
                        storageDegraded = true,
                    )
                }
            }
        }
        capture("06-placeholder-night-360-en-fontscale13-degraded")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun componentSheetDay() {
        render(dark = false) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(KXDimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(KXDimens.SpaceMd),
            ) {
                KXOrb(size = KXDimens.OrbMedium)
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KXButton(label = "Primary", onClick = {})
                        KXButton(
                            label = "Secondary",
                            onClick = {},
                            style = KXButtonStyle.Secondary,
                        )
                        KXButton(
                            label = "Unavailable",
                            onClick = {},
                            enabled = false,
                            disabledReason = "Requires the tunnel engine.",
                        )
                    }
                }
                IconAction(
                    icon = Icons.Filled.Close,
                    contentDescription = "Close",
                    onClick = {},
                )
            }
        }
        capture("07-component-sheet-day-360-en")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun launchScreenNight() {
        val viewModel = LaunchViewModel(
            FakeLaunchRepository.answering(LaunchSnapshot(onboardingCompleted = false)),
        )
        render(dark = true) {
            LaunchScreen(viewModel = viewModel, onNavigate = { _, _ -> })
        }
        capture("08-launch-night-360-en")
    }

    /**
     * The store has not answered, so the gate must stay on the launch screen. If this
     * ever navigates, the "no assumed state" rule is broken.
     */
    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
    fun launchScreenHoldsWhileTheStoreIsSilent() {
        var navigated = false
        val viewModel = LaunchViewModel(FakeLaunchRepository.silent())
        render(dark = true) {
            LaunchScreen(viewModel = viewModel, onNavigate = { _, _ -> navigated = true })
        }
        compose.waitForIdle()
        check(!navigated) { "launch gate navigated before the store answered" }
        capture("09-launch-night-360-en-loading")
    }

    private class FakeLaunchRepository(
        private val flow: MutableStateFlow<LaunchSnapshot>?,
    ) : LaunchStateRepository {
        override val snapshot: Flow<LaunchSnapshot>
            get() = flow ?: emptyFlow()

        override suspend fun completeOnboarding() {
            flow?.let { it.emit(it.value.copy(onboardingCompleted = true)) }
        }

        companion object {
            fun answering(snapshot: LaunchSnapshot) =
                FakeLaunchRepository(MutableStateFlow(snapshot))

            /** Models a store that has not answered yet — the Loading condition. */
            fun silent() = FakeLaunchRepository(null)
        }
    }
}
