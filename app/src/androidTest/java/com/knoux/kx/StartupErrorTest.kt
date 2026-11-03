package com.knoux.kx

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.knoux.kx.startup.StartupState
import com.knoux.kx.ui.KxApp
import com.knoux.kx.ui.KxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartupErrorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun storageErrorOffersRealRetry() {
        var retries = 0
        compose.setContent { KxTheme { KxApp(StartupState.ReadFailure) { retries++ } } }
        compose.onNodeWithTag("retry").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
