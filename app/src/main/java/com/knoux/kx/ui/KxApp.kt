package com.knoux.kx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.knoux.kx.R
import com.knoux.kx.startup.LaunchDestination
import com.knoux.kx.startup.StartupState

private const val FIRST_RUN = "first-run"
private const val RETURNING = "returning"
private const val ABOUT = "about"

@Composable
fun KxApp(state: StartupState, retry: () -> Unit) {
    BrandBackdrop(Modifier.fillMaxSize()) {
        when (state) {
            StartupState.Loading -> LaunchLayout {
                Text(stringResource(R.string.launch_loading), Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                }, color = MaterialTheme.colorScheme.onBackground)
            }
            StartupState.ReadFailure -> LaunchLayout {
                LaunchCard(R.string.launch_error_title, R.string.launch_error_body) {
                    KXButton(stringResource(R.string.retry), retry, Modifier.testTag("retry"))
                }
            }
            is StartupState.Ready -> ReadyNavigation(state.destination)
        }
    }
}

@Composable
private fun ReadyNavigation(destination: LaunchDestination) {
    val nav = rememberNavController()
    val start = if (destination == LaunchDestination.FirstRun) FIRST_RUN else RETURNING
    NavHost(navController = nav, startDestination = start) {
        composable(FIRST_RUN) {
            Landing(false) { nav.navigate(ABOUT) { launchSingleTop = true } }
        }
        composable(RETURNING) {
            Landing(true) { nav.navigate(ABOUT) { launchSingleTop = true } }
        }
        composable(ABOUT) {
            LaunchLayout {
                LaunchCard(R.string.foundation_title, R.string.foundation_body) {
                    KXButton(stringResource(R.string.back), { nav.popBackStack() }, Modifier.testTag("back"))
                }
            }
        }
    }
}

@Composable
private fun Landing(returning: Boolean, onDetails: () -> Unit) {
    LaunchLayout {
        LaunchCard(
            if (returning) R.string.returning_title else R.string.first_run_title,
            if (returning) R.string.returning_body else R.string.first_run_body,
            Modifier.testTag(if (returning) RETURNING else FIRST_RUN),
        ) {
            KXButton(stringResource(R.string.foundation_details), onDetails, Modifier.testTag("about"))
        }
        Text(stringResource(R.string.build_status), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LaunchLayout(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KXOrb(Modifier.sizeIn(maxWidth = 232.dp, maxHeight = 232.dp))
            Text(stringResource(R.string.brand_name), color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light, letterSpacing = 5.sp)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.brand_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
        }
        content()
    }
}

@Composable
private fun LaunchCard(title: Int, body: Int, modifier: Modifier = Modifier, actions: @Composable () -> Unit) {
    GlassCard(modifier.widthIn(max = 460.dp).fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(title), Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            actions()
        }
    }
}

@Preview(name = "English day 360", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun DayPreview() = KxTheme(false) { KxApp(StartupState.Ready(LaunchDestination.FirstRun), {}) }

@Preview(name = "Arabic night 411 large type", locale = "ar", widthDp = 411, heightDp = 891, fontScale = 1.3f)
@Composable
private fun NightPreview() = KxTheme(true) { KxApp(StartupState.Ready(LaunchDestination.FirstRun), {}) }
