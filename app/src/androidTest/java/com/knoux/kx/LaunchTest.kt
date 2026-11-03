package com.knoux.kx

import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kx.startup.PreferencesStartupRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class LaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun splashExitsAndNavigationSurvivesActivityRecreation() {
        awaitTag("first-run")
        compose.onNodeWithTag("first-run").assertExists()
        capture("first-run")
        compose.onNodeWithTag("about").performScrollTo().performClick()
        compose.onNodeWithTag("back").performScrollTo().assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        awaitTag("back")
        compose.onNodeWithTag("back").performScrollTo().assertIsDisplayed().performClick()
        awaitTag("first-run")
        compose.onNodeWithTag("first-run").assertExists()
        assertFalse(runBlocking {
            PreferencesStartupRepository(compose.activity).isOnboardingAccepted()
        })
    }

    @Test fun relaunchStillShowsFirstRunWithoutVpnPermissions() {
        compose.activityRule.scenario.recreate()
        awaitTag("first-run")
        compose.onNodeWithTag("first-run").assertExists()
        val packageInfo = compose.activity.packageManager.getPackageInfo(compose.activity.packageName, 4096)
        assertFalse(packageInfo.requestedPermissions.orEmpty().any {
            it == "android.permission.INTERNET" || it == "android.permission.BIND_VPN_SERVICE"
        })
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val directory = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        directory.mkdirs()
        val configuration = context.resources.configuration
        val variant = "${configuration.locales[0]}-${configuration.uiMode}-${configuration.screenWidthDp}-${configuration.fontScale}"
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, "$name-$variant.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { bitmap.recycle() }
    }
}
