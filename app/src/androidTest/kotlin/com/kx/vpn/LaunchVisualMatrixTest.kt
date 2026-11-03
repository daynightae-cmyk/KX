package com.kx.vpn

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kx.vpn.ui.placeholder.PlaceholderScreen
import com.kx.vpn.ui.theme.KXMotionSpec
import com.kx.vpn.ui.theme.KXTheme
import com.kx.vpn.ui.theme.LocalKXMotion
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Real device Compose renders; not an end-to-end locale/settings switch or visual approval. */
@RunWith(Parameterized::class)
class LaunchVisualMatrixTest(
    private val language: String,
    private val dark: Boolean,
    private val widthDp: Int,
    private val fontScale: Float,
) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun firstRunRemainsScrollableAndHonestAcrossSupportedVariants() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
            screenWidthDp = widthDp
            this.fontScale = this@LaunchVisualMatrixTest.fontScale
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        val localized = context.createConfigurationContext(configuration)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(context.resources.displayMetrics.density, fontScale),
                LocalLayoutDirection provides if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalKXMotion provides KXMotionSpec.Reduced,
            ) {
                KXTheme(darkTheme = dark) {
                    Box(Modifier.requiredWidth(widthDp.dp).fillMaxHeight()) {
                        PlaceholderScreen(
                            title = stringResource(R.string.welcome_title),
                            body = stringResource(R.string.welcome_body),
                            actionLabel = stringResource(R.string.welcome_action),
                            actionDisabledReason = stringResource(R.string.welcome_action_unavailable),
                            storageDegraded = false,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val variant = "$language-${if (dark) "night" else "day"}-${widthDp}dp-font$fontScale"
        capture("first-run-top-$variant")
        compose.onNodeWithText(localized.getString(R.string.welcome_action))
            .performScrollTo()
            .assertIsNotEnabled()
            .assertHeightIsAtLeast(48.dp)
        capture("first-run-action-$variant")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        check(directory.isDirectory || directory.mkdirs())
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-night={1}-{2}dp-font={3}")
        fun variants(): List<Array<Any>> = listOf("en", "ar").flatMap { language ->
            listOf(false, true).flatMap { dark ->
                listOf(360, 411).flatMap { width ->
                    listOf(1f, 1.3f).map { scale -> arrayOf<Any>(language, dark, width, scale) }
                }
            }
        }
    }
}
