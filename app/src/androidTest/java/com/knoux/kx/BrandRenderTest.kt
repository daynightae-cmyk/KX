package com.knoux.kx

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Environment
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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.knoux.kx.startup.LaunchDestination
import com.knoux.kx.startup.StartupState
import com.knoux.kx.ui.KxApp
import com.knoux.kx.ui.KxTheme
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BrandRenderTest(
    private val language: String,
    private val dark: Boolean,
    private val width: Int,
    private val fontScale: Float,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun brandAndFirstRunHaveResponsiveNativeLayout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            screenWidthDp = width
            this.fontScale = this@BrandRenderTest.fontScale
        }
        val localized = context.createConfigurationContext(configuration)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(context.resources.displayMetrics.density, fontScale),
                LocalLayoutDirection provides if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                KxTheme(dark) {
                    Box(Modifier.requiredWidth(width.dp).fillMaxHeight()) {
                        KxApp(StartupState.Ready(LaunchDestination.FirstRun), {})
                    }
                }
            }
        }
        compose.waitForIdle()
        val directory = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        directory.mkdirs()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "brand-$language-$dark-$width-$fontScale.png").outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        compose.onNodeWithTag("about").performScrollTo().assertHeightIsAtLeast(48.dp)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-dark={1}-{2}dp-font={3}")
        fun variants(): List<Array<Any>> = listOf("en", "ar").flatMap { locale ->
            listOf(false, true).flatMap { dark ->
                listOf(360, 411).flatMap { width ->
                    listOf(1f, 1.3f).map { scale -> arrayOf<Any>(locale, dark, width, scale) }
                }
            }
        }
    }
}
