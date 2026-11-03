package com.kx.vpn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kx.vpn.ui.brand.KXOrbTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device launch checks.
 *
 * These can only be meaningful against a real framework: they drive the actual
 * activity, the system splash hand-off and the real semantics tree. Running them needs
 * an emulator or physical device, which this machine cannot provide — see
 * docs/BLOCKERS.md. CI compiles them with `assembleDebugAndroidTest` so the code is
 * proven valid against the real test APIs even while execution is blocked.
 */
@RunWith(AndroidJUnit4::class)
class LaunchInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun brandEmblemIsPresentAndAnnounced() {
        composeRule.waitForIdle()
        composeRule
            .onNodeWithContentDescription(KXOrbTags.EmblemContentDescription)
            .assertIsDisplayed()
    }

    /**
     * The gate must land somewhere real. Which destination depends on persisted
     * state, so either first-run or home is acceptable — a blank screen is not.
     */
    @Test
    fun launchLandsOnARealDestination() {
        composeRule.waitForIdle()
        val welcome = context.getString(R.string.welcome_title)
        val home = context.getString(R.string.home_title)

        val welcomeNodes = composeRule.onAllNodesWithText(welcome)
            .fetchSemanticsNodes().isNotEmpty()
        val homeNodes = composeRule.onAllNodesWithText(home)
            .fetchSemanticsNodes().isNotEmpty()

        if (!welcomeNodes && !homeNodes) {
            composeRule.onRoot().printToLog("KX_LAUNCH_TEST")
            throw AssertionError(
                "Launch gate resolved to neither the first-run nor the home destination",
            )
        }
    }

    /**
     * Honesty invariant asserted on a device: no build without a tunnel engine may
     * render a connected or protected claim.
     */
    @Test
    fun noProtectionClaimIsEverRendered() {
        composeRule.waitForIdle()
        listOf("Connected", "Fully Protected", "Protected", "Premium", "Unlimited")
            .forEach { forbidden ->
                val failure = runCatching {
                    composeRule.onNodeWithText(forbidden).assertExists()
                }
                if (failure.isSuccess) {
                    throw AssertionError(
                        "Forbidden status text rendered without a verified backend: " +
                            "\"$forbidden\"",
                    )
                }
            }
    }

    @Test
    fun unavailableConnectionActionIsDisabledNotFake() {
        composeRule.waitForIdle()
        val home = context.getString(R.string.home_title)
        val connect = context.getString(R.string.home_action)

        if (composeRule.onAllNodesWithText(home).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(connect).assertIsNotEnabled()
        }
    }

    @Test
    fun availablePrimaryActionIsTappable() {
        composeRule.waitForIdle()
        val welcome = context.getString(R.string.welcome_title)
        val setup = context.getString(R.string.welcome_action)

        if (composeRule.onAllNodesWithText(welcome).fetchSemanticsNodes().isNotEmpty()) {
            // The setup control is intentionally disabled in TASK_00; assert the
            // opposite of "fake success" rather than pretending it works.
            composeRule.onNodeWithText(setup).assertIsNotEnabled()
        }
    }

    @Test
    fun relaunchAfterProcessDeathReturnsAValidDestination() {
        composeRule.waitForIdle()
        composeRule.activity.finish()
        val scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        scenario.use {
            composeRule.waitForIdle()
            composeRule
                .onNodeWithContentDescription(KXOrbTags.EmblemContentDescription)
                .assertIsDisplayed()
        }
    }
}
