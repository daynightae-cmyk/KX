package com.kx.vpn

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kx.vpn.data.TunnelSessionState
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.ui.home.HomeScreen
import com.kx.vpn.ui.theme.KXMotionSpec
import com.kx.vpn.ui.theme.KXTheme
import com.kx.vpn.ui.theme.LocalKXMotion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Render fixture only; never creates a VPN or treats fixture time as network evidence. */
@RunWith(AndroidJUnit4::class)
class SessionDurationInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun durationIsReachableAndExplicitlyDoesNotClaimInternetVerification() {
        compose.setContent {
            CompositionLocalProvider(LocalKXMotion provides KXMotionSpec.Reduced) {
                KXTheme {
                    HomeScreen(
                        session = TunnelSessionState(tunnelState = TunnelState.CONNECTED,
                            hasProfile = true, startedAtElapsedMillis = 0L, elapsedMillis = 61_000L),
                        onConnect = {}, onDisconnect = {},
                    )
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.home_tunnel_duration, "00:01:01"))
            .performScrollTo().assertIsDisplayed()
    }
}
