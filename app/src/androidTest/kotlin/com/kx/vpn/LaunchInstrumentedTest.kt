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
import kotlinx.coroutines.flow.first
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
        awaitDestination()
        composeRule
            .onNodeWithContentDescription(KXOrbTags.EmblemContentDescription)
            .assertIsDisplayed()
        captureLaunch("brand-destination")
    }

    /**
     * The gate must land somewhere real. Which destination depends on persisted
     * state, so either first-run or home is acceptable — a blank screen is not.
     */
    @Test
    fun launchLandsOnARealDestination() {
        awaitDestination()
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
        awaitDestination()
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
        awaitDestination()
        val home = context.getString(R.string.home_title)
        val connect = context.getString(R.string.home_action)

        if (composeRule.onAllNodesWithText(home).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText(connect).assertIsNotEnabled()
        }
    }

    @Test
    fun welcomeSetupActionIsDisabledNotFake() {
        awaitDestination()
        val welcome = context.getString(R.string.welcome_title)
        val setup = context.getString(R.string.welcome_action)

        if (composeRule.onAllNodesWithText(welcome).fetchSemanticsNodes().isNotEmpty()) {
            // The setup control is intentionally disabled in TASK_00; assert the
            // opposite of "fake success" rather than pretending it works.
            composeRule.onNodeWithText(setup).assertIsNotEnabled()
        }
    }

    @Test
    fun activityRecreationReturnsAValidDestinationWithoutAcceptingSetup() {
        awaitDestination()
        val before = launchSnapshot()
        composeRule.activityRule.scenario.recreate()
        awaitDestination()
        val after = launchSnapshot()
        org.junit.Assert.assertEquals(before, after)
        composeRule
            .onNodeWithContentDescription(KXOrbTags.EmblemContentDescription)
            .assertIsDisplayed()
    }

    private fun captureLaunch(name: String) {
        composeRule.waitForIdle()
        val directory = requireNotNull(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES))
        check(directory.isDirectory || directory.mkdirs())
        val config = context.resources.configuration
        val variant = "${config.locales[0]}-${config.uiMode}-${config.screenWidthDp}-${config.fontScale}"
        val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            java.io.File(directory, "$name-$variant.png").outputStream().use {
                check(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            screenshot.recycle()
        }
    }

    private fun launchSnapshot() = kotlinx.coroutines.runBlocking {
        kotlinx.coroutines.withTimeout(5_000) {
            com.kx.vpn.data.createLaunchStateRepository(context).snapshot.first()
        }
    }

    private fun awaitDestination() {
        val welcome = context.getString(R.string.welcome_title)
        val home = context.getString(R.string.home_title)
        // Compose idleness alone does not wait for asynchronous DataStore IO.
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText(welcome).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText(home).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
