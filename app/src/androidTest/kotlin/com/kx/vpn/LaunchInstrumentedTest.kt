package com.kx.vpn

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kx.vpn.ui.brand.KXOrbTags
import com.kx.vpn.ui.launch.KXEntryTags
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device launch and first-run checks.
 *
 * These are executed automatically by the `KX Android Device Runtime` workflow on
 * emulator API 26 and API 35 for every PR and every merge to main, and the tests write
 * real framebuffer screenshots as they go. They are not compile-only.
 */
@RunWith(AndroidJUnit4::class)
class LaunchInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(resId: Int) = context.getString(resId)

    private fun hasNodes(value: String) =
        composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()

    /**
     * DataStore and the 3D intro must resolve before the user can tap KX.
     * This helper uses the actual click; the app must never navigate itself.
     */
    private fun awaitDestination() {
        val welcomeSub = text(R.string.welcome_wordmark_sub)
        val home = text(R.string.home_title)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasNodes(text(R.string.launch_enter_hint)) &&
                runCatching {
                    composeRule.onNodeWithTag(KXEntryTags.EnterOrb).assertIsEnabled()
                }.isSuccess
        }
        composeRule.onNodeWithTag(KXEntryTags.EnterOrb).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasNodes(welcomeSub) || hasNodes(home)
        }
    }

    @Test
    fun launchDoesNotAutoEnterWhenReady() {
        val welcomeSub = text(R.string.welcome_wordmark_sub)
        val home = text(R.string.home_title)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasNodes(text(R.string.launch_enter_hint)) &&
                runCatching {
                    composeRule.onNodeWithTag(KXEntryTags.EnterOrb).assertIsEnabled()
                }.isSuccess
        }
        org.junit.Assert.assertFalse("welcome must require a tap", hasNodes(welcomeSub))
        org.junit.Assert.assertFalse("home must require a tap", hasNodes(home))
        composeRule.onNodeWithTag(KXEntryTags.EnterOrb).assertIsDisplayed()
        captureLaunch("interactive-launch-gate")
    }

    private fun onWelcome() = hasNodes(text(R.string.welcome_wordmark_sub))

    @Test
    fun brandEmblemIsPresentAndAnnounced() {
        awaitDestination()
        composeRule
            .onNodeWithContentDescription(KXOrbTags.EmblemContentDescription)
            .assertIsDisplayed()
        captureLaunch("brand-destination")
    }

    @Test
    fun launchLandsOnARealDestination() {
        awaitDestination()
        if (!onWelcome() && !hasNodes(text(R.string.home_title))) {
            composeRule.onRoot().printToLog("KX_LAUNCH_TEST")
            throw AssertionError(
                "Launch gate resolved to neither the first-run nor the home destination",
            )
        }
    }

    /**
     * Honesty invariant asserted on a device. The reference artwork shows "Borderless"
     * and a "Premium" badge; the task contract bans reproducing those as claims, so they
     * are on the forbidden list alongside connection and protection states.
     */
    @Test
    fun noProtectionOrMarketingClaimIsEverRendered() {
        awaitDestination()
        listOf(
            "Connected", "Fully Protected", "Protected", "Premium",
            "Unlimited", "No logs", "Borderless",
        ).forEach { forbidden ->
            val present = runCatching {
                composeRule.onNodeWithText(forbidden, substring = true).assertExists()
            }.isSuccess
            if (present) {
                throw AssertionError(
                    "Forbidden status text rendered without a verified backend: \"$forbidden\"",
                )
            }
        }
    }

    @Test
    fun bothFirstRunControlsAreRealAndEnabled() {
        awaitDestination()
        if (!onWelcome()) return // Returning user: the welcome step is correctly absent.

        composeRule.onNodeWithText(text(R.string.welcome_get_started)).assertIsEnabled()
        composeRule.onNodeWithText(text(R.string.welcome_import_config)).assertIsEnabled()
        captureLaunch("welcome-controls")
    }

    @Test
    fun getStartedReachesTheDisclosureStep() {
        awaitDestination()
        if (!onWelcome()) return

        composeRule.onNodeWithText(text(R.string.welcome_get_started)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            hasNodes(text(R.string.onboarding_title))
        }
        captureLaunch("onboarding")
    }

    @Test
    fun importConfigReachesTheInspectionScreen() {
        awaitDestination()
        if (!onWelcome()) return

        composeRule.onNodeWithText(text(R.string.welcome_import_config)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            hasNodes(text(R.string.import_choose_file))
        }
        composeRule.onNodeWithText(text(R.string.import_choose_file)).assertIsEnabled()
        captureLaunch("import-entry")
    }

    /**
     * Continue must stay disabled until the disclosure is acknowledged. A pre-enabled
     * Continue would let onboarding be recorded as accepted without the user having read
     * what a VPN does and does not do for them.
     */
    @Test
    fun continueIsLockedUntilDisclosureAcknowledged() {
        awaitDestination()
        if (!onWelcome()) return

        composeRule.onNodeWithText(text(R.string.welcome_get_started)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            hasNodes(text(R.string.onboarding_title))
        }
        composeRule.onNodeWithText(text(R.string.onboarding_continue)).assertIsNotEnabled()
    }

    @Test
    fun unavailableConnectionActionIsDisabledNotFake() {
        awaitDestination()
        if (!hasNodes(text(R.string.home_title))) return // First run: Home is not on screen.

        composeRule.onNodeWithText(text(R.string.home_connect)).assertIsNotEnabled()
    }

    /**
     * TASK_01 must not raise a VPN permission prompt or any other dialog on first run.
     * The manifest declares zero permissions, so there is nothing to ask for; this
     * asserts the app reaches a destination rather than blocking on a grant sheet.
     */
    @Test
    fun noUnexpectedPermissionDialogOnFirstRun() {
        awaitDestination()
        listOf("Allow", "While using the app", "Use app and files", "Grant").forEach { label ->
            val present = runCatching {
                composeRule.onNodeWithText(label).assertExists()
            }.isSuccess
            if (present) {
                throw AssertionError("An unexpected permission dialog appeared: $label")
            }
        }
    }

    @Test
    fun activityRecreationBeforeEntryStillRequiresAnExplicitTap() {
        val hint = text(R.string.launch_enter_hint)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasNodes(hint) &&
                runCatching {
                    composeRule.onNodeWithTag(KXEntryTags.EnterOrb).assertIsEnabled()
                }.isSuccess
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasNodes(hint) &&
                runCatching {
                    composeRule.onNodeWithTag(KXEntryTags.EnterOrb).assertIsEnabled()
                }.isSuccess
        }
        org.junit.Assert.assertFalse(
            "recreation must not auto-enter welcome",
            hasNodes(text(R.string.welcome_wordmark_sub)),
        )
        org.junit.Assert.assertFalse(
            "recreation must not auto-enter home",
            hasNodes(text(R.string.home_title)),
        )
        composeRule.onNodeWithTag(KXEntryTags.EnterOrb).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasNodes(text(R.string.welcome_wordmark_sub)) ||
                hasNodes(text(R.string.home_title))
        }
    }

    @Test
    fun activityRecreationReturnsAValidDestinationWithoutAcceptingSetup() {
        awaitDestination()
        val before = launchSnapshot()
        composeRule.activityRule.scenario.recreate()
        // Navigation restores the destination already entered by the user.
        // A new tap must NOT be required when the system recreates the Activity
        // after the welcome/home route was already reached.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasNodes(text(R.string.welcome_wordmark_sub)) ||
                hasNodes(text(R.string.home_title))
        }
        val after = launchSnapshot()
        org.junit.Assert.assertEquals(
            "recreation must not silently complete onboarding",
            before,
            after,
        )
        composeRule
            .onNodeWithContentDescription(KXOrbTags.EmblemContentDescription)
            .assertIsDisplayed()
    }

    private fun captureLaunch(name: String) {
        composeRule.waitForIdle()
        val directory = requireNotNull(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
        )
        check(directory.isDirectory || directory.mkdirs())
        val config = context.resources.configuration
        val variant =
            "${config.locales[0]}-${config.uiMode}-${config.screenWidthDp}-${config.fontScale}"
        val screenshot = requireNotNull(
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot(),
        )
        try {
            java.io.File(directory, "$name-$variant.png").outputStream().use { stream ->
                check(
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream),
                )
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
}
