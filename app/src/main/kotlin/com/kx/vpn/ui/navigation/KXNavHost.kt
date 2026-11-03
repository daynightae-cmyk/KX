package com.kx.vpn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kx.vpn.R
import com.kx.vpn.domain.LaunchDestination
import com.kx.vpn.ui.launch.LaunchScreen
import com.kx.vpn.ui.launch.LaunchViewModel
import com.kx.vpn.ui.placeholder.PlaceholderScreen

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

        composable(
            route = "${KXRoute.Welcome}?{${KXRoute.ArgStorageDegraded}}",
            arguments = listOf(degradedArg),
        ) { entry ->
            PlaceholderScreen(
                title = stringResource(R.string.welcome_title),
                body = stringResource(R.string.welcome_body),
                actionLabel = stringResource(R.string.welcome_action),
                actionDisabledReason = stringResource(R.string.welcome_action_unavailable),
                storageDegraded =
                    entry.arguments?.getBoolean(KXRoute.ArgStorageDegraded) ?: false,
            )
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
