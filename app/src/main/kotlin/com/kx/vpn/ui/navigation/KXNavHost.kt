package com.kx.vpn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kx.vpn.R
import com.kx.vpn.data.LaunchStateRepository
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
            ImportEntryScreen(viewModel = importViewModel)
        }

        composable(
            route = "${KXRoute.Home}?{${KXRoute.ArgStorageDegraded}}",
            arguments = listOf(degradedArg),
        ) { entry ->
            PlaceholderScreen(
                title = stringResource(R.string.home_title),
                body = stringResource(R.string.home_body),
                actionLabel = stringResource(R.string.home_action),
                actionDisabledReason = stringResource(R.string.home_action_unavailable),
                storageDegraded =
                    entry.arguments?.getBoolean(KXRoute.ArgStorageDegraded) ?: false,
            )
        }
    }
}

internal class OnboardingViewModelFactory(
    private val repository: LaunchStateRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        requireNotNull(OnboardingViewModel(repository) as? T) {
            "Requested $modelClass from an Onboarding-only factory"
        }
}
