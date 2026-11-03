package com.kx.vpn

import android.graphics.Bitmap
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kx.vpn.data.session.WireGuardSessionRepository
import com.kx.vpn.ui.home.HomeScreen
import com.kx.vpn.ui.home.HomeViewModel
import com.kx.vpn.ui.theme.KXMotionSpec
import com.kx.vpn.ui.theme.KXTheme
import com.kx.vpn.ui.theme.LocalKXMotion
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Android rendering; never opens a consent prompt or contacts a gateway. */
@RunWith(AndroidJUnit4::class)
class HomeDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun unconfiguredHomeCannotConnectAndImportRemainsReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(!File(context.noBackupFilesDir, "owned-tunnel.enc").exists()) {
            "Run this test on a clean dedicated test device"
        }
        val model = HomeViewModel(WireGuardSessionRepository(context))
        compose.setContent {
            CompositionLocalProvider(LocalKXMotion provides KXMotionSpec.Reduced) {
                KXTheme { HomeScreen(model) }
            }
        }
        compose.onNodeWithText(context.getString(R.string.tunnel_unconfigured)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.tunnel_connect))
            .performScrollTo().assertIsNotEnabled()
        capture("home-unconfigured")
        compose.onNodeWithText(context.getString(R.string.tunnel_save_config))
            .performScrollTo().assertIsDisplayed().assertIsEnabled()
        capture("home-owned-import")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val dir = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        check(dir.exists() || dir.mkdirs())
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(dir, "$name.png").outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally { bitmap.recycle() }
    }
}
