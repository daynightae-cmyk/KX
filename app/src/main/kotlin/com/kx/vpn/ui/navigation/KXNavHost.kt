package com.kx.vpn.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kx.vpn.R
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.data.ProfileRepository
import com.kx.vpn.data.TunnelSessionManager
import com.kx.vpn.domain.LaunchDestination
import com.kx.vpn.ui.imports.ImportEntryScreen
import com.kx.vpn.ui.imports.ImportEntryViewModel
import com.kx.vpn.ui.launch.LaunchScreen
import com.kx.vpn.ui.launch.LaunchViewModel
import com.kx.vpn.ui.onboarding.OnboardingScreen
import com.kx.vpn.ui.onboarding.OnboardingViewModel
import com.kx.vpn.ui.placeholder.PlaceholderScreen
import com.kx.vpn.ui.welcome.WelcomeScreen

/**
 * Root graph.
 *
 * The launch node is popped inclusively once it resolves, so "back" from a real
 * destination never replays the splash — the behaviour the contract asks for when it
 * says navigation must have real back semantics.
 */
@Composable
fun KXNavHost(
    navController: NavHostController,
    launchViewModel: LaunchViewModel,
    repository: LaunchStateRepository,
    modifier: Modifier = Modifier,
    sessionManager: TunnelSessionManager? = null,
    profileRepository: ProfileRepository? = null,
    onConnect: () -> Unit = {},
) {
    val degradedArg = navArgument(KXRoute.ArgStorageDegraded) {
        type = NavType.BoolType
        defaultValue = false
    }

    NavHost(
        navController = navController,
        startDestination = KXRoute.Launch,
        modifier = modifier,
    ) {
        composable(KXRoute.Launch) {
            LaunchScreen(
                viewModel = launchViewModel,
                onNavigate = { destination, storageDegraded ->
                    val route = when (destination) {
                        LaunchDestination.Welcome -> KXRoute.Welcome
                        LaunchDestination.Home -> KXRoute.Home
                    }
                    navController.navigate(
                        "$route?${KXRoute.ArgStorageDegraded}=$storageDegraded",
                    ) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(KXRoute.Welcome) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(KXRoute.Onboarding) },
                onImportConfig = { navController.navigate(KXRoute.Import) },
            )
        }

        composable(KXRoute.Onboarding) {
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModelFactory(repository),
            )
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onCompleted = {
                    // Home is the destination after acceptance, and the onboarding and
                    // welcome nodes are removed so back from Home returns the user to
                    // the app root rather than into their own setup flow.
                    navController.navigate(KXRoute.Home) {
                        popUpTo(KXRoute.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(KXRoute.Import) {
            val importViewModel: ImportEntryViewModel = viewModel()
            ImportEntryScreen(
                viewModel = importViewModel,
                onSaveProfile = { configText ->
                    val repository = profileRepository
                    if (repository != null) {
                        // The write goes through the keystore-backed vault, so the
                        // private key never touches the disk in the clear.
                        repository.save(DEFAULT_PROFILE_ID, configText)
                        sessionManager?.refreshProfilePresence()
                    }
                },
            )
        }

        composable(
            route = "${KXRoute.Home}?{${KXRoute.ArgStorageDegraded}}",
            arguments = listOf(degradedArg),
        ) { entry ->
            val sessionState = sessionManager?.state?.collectAsStateWithLifecycle()?.value
            HomeSurface(
                sessionState = sessionState,
                onConnect = onConnect,
                onDisconnect = { sessionManager?.disconnect() },
            )
        }
    }
}

/**
 * The home destination.
 *
 * Until TASK_03 supplies the remaining tabs, the screen shows the real connection
 * controls and says plainly that the other sections do not exist yet. The contract
 * allows "temporarily explicitly labeled unavailable navigation", and forbids a
 * navigation that implies an active service which is not there.
 */
@Composable
private fun HomeSurface(
    sessionState: com.kx.vpn.data.TunnelSessionState?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    if (sessionState == null) {
        PlaceholderScreen(
            title = stringResource(R.string.home_title),
            body = stringResource(R.string.home_tab_coming_soon),
            actionLabel = stringResource(R.string.home_connect),
            actionDisabledReason = stringResource(R.string.home_action_unavailable),
            storageDegraded = false,
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        com.kx.vpn.ui.home.HomeScreen(
            session = sessionState,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            modifier = Modifier.weight(1f),
        )
        com.kx.vpn.ui.home.HomeTabBar(
            selected = com.kx.vpn.ui.home.HomeTab.Home,
            onSelect = { },
        )
    }
}

/** The single profile slot until TASK_09 introduces real profile management. */
const val DEFAULT_PROFILE_ID = "default"

internal class OnboardingViewModelFactory(
    private val repository: LaunchStateRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        requireNotNull(OnboardingViewModel(repository) as? T) {
            "Requested $modelClass from an Onboarding-only factory"
        }
}
