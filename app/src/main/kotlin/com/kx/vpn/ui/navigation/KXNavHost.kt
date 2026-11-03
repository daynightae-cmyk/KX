package com.kx.vpn.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
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
import com.kx.vpn.data.vault.VaultResult
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.servers.EndpointIdentity
import com.kx.vpn.domain.servers.EndpointValidator
import com.kx.vpn.domain.servers.ProfileEndpointReader
import com.kx.vpn.domain.servers.ServerSwitchContext
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
    catalogueRepository: com.kx.vpn.data.EndpointCatalogueRepository? = null,
    latencyProbe: com.kx.vpn.platform.TcpLatencyProbe? = null,
    switchController: com.kx.vpn.data.EndpointSwitchController? = null,
    sessionTracker: com.kx.vpn.data.ConnectedSessionTracker? = null,
    networkAvailability: com.kx.vpn.platform.NetworkAvailability? = null,
    onConnect: () -> Unit = {},
) {
    // The catalogue writes are suspend functions; the navigation graph needs a scope
    // that outlives a single composition.
    val scope = androidx.compose.runtime.rememberCoroutineScope()
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
                onSaveProfile = { configText, suggestedLabel ->
                    val repository = profileRepository
                    if (repository != null) {
                        val endpoint = ProfileEndpointReader.read(configText)
                        val profileId = endpoint
                            ?.let { EndpointIdentity.profileId(it.host, it.port, it.publicKey) }
                            ?: fallbackProfileId(configText)
                        // The write goes through the keystore-backed vault, so the
                        // private key never touches the disk in the clear.
                        val saved = repository.save(profileId, configText)
                        // The catalogue entry is read from the configuration the user
                        // supplied; nothing about it is invented.
                        if (saved is VaultResult.Success && endpoint != null) {
                            val label = suggestedLabel
                                // A provider that reports no name leaves the host as the
                                // only honest label available.
                                ?.trim().orEmpty()
                                .ifBlank { endpoint.host }
                                .take(EndpointValidator.MAX_LABEL_LENGTH)
                            val profile = EndpointProfile(
                                id = profileId,
                                displayName = label,
                                countryCode = null,
                                host = endpoint.host,
                                port = endpoint.port,
                                profileId = profileId,
                            )
                            if (EndpointValidator.validate(profile).isEmpty()) {
                                scope.launch { catalogueRepository?.upsert(profile) }
                            }
                        }
                        sessionManager?.refreshProfilePresence()
                    }
                },
            )
        }

        composable(KXRoute.Servers) {
            val serversViewModel: com.kx.vpn.ui.servers.ServersViewModel = viewModel(
                factory = ServersViewModelFactory(
                    repository = catalogueRepository,
                    probe = latencyProbe,
                    switchController = switchController,
                    network = networkAvailability,
                ),
            )
            val listState by serversViewModel.state.collectAsStateWithLifecycle()
            val measuring by serversViewModel.isMeasuring.collectAsStateWithLifecycle()
            val offline by serversViewModel.isOffline.collectAsStateWithLifecycle()
            val query by serversViewModel.currentQuery.collectAsStateWithLifecycle()
            val activeFilter by serversViewModel.currentFilter.collectAsStateWithLifecycle()
            val switchContext = serversViewModel.switch
                ?.collectAsStateWithLifecycle()
                ?.value ?: ServerSwitchContext()
            // Opening the list measures it. The reference screen shows a latency per row,
            // and the only honest way to have one is to take it — against the user's own
            // endpoints, bounded to one batch with a concurrency cap and a per-endpoint
            // timeout, and not repeated while a batch is already running.
            androidx.compose.runtime.LaunchedEffect(Unit) {
                serversViewModel.measure()
            }
            com.kx.vpn.ui.servers.SelectServerScreen(
                state = listState,
                query = query,
                selectedFilter = activeFilter,
                enabledFilters = setOf(
                    com.kx.vpn.domain.servers.ServerFilter.All,
                    com.kx.vpn.domain.servers.ServerFilter.Fastest,
                ),
                isMeasuring = measuring,
                isOffline = offline,
                switchState = switchContext.state,
                switchFailure = switchContext.failure,
                onQueryChange = serversViewModel::setQuery,
                onFilterChange = serversViewModel::setFilter,
                onSelect = serversViewModel::select,
                onToggleFavorite = serversViewModel::toggleFavorite,
                onDismissSwitchFailure = serversViewModel::acknowledgeSwitchFailure,
                onImport = { navController.navigate(KXRoute.Import) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "${KXRoute.Home}?{${KXRoute.ArgStorageDegraded}}",
            arguments = listOf(degradedArg),
        ) { entry ->
            val sessionState = sessionManager?.state?.collectAsStateWithLifecycle()?.value
            val connectedSession = sessionTracker?.state?.collectAsStateWithLifecycle()?.value
            HomeSurface(
                sessionState = sessionState,
                connectedSession = connectedSession,
                onConnect = onConnect,
                onDisconnect = { sessionManager?.disconnect() },
                onOpenServers = { navController.navigate(KXRoute.Servers) },
                onPingAgain = { sessionTracker?.pingEndpoint() },
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
    connectedSession: com.kx.vpn.data.ConnectedSessionState?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenServers: () -> Unit,
    onPingAgain: () -> Unit,
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
            connectedSession = connectedSession,
            onChangeServer = onOpenServers,
            onPingAgain = onPingAgain,
            modifier = Modifier.weight(1f),
        )
        com.kx.vpn.ui.home.HomeTabBar(
            selected = com.kx.vpn.ui.home.HomeTab.Home,
            onSelect = { tab ->
                if (tab == com.kx.vpn.ui.home.HomeTab.Servers) {
                    onOpenServers()
                }
            },
            enabledTabs = setOf(com.kx.vpn.ui.home.HomeTab.Servers),
        )
    }
}

/**
 * The id for a configuration that declares no usable peer endpoint.
 *
 * It is still the user's configuration and still gets stored, but it cannot become a
 * catalogue entry: a server list row has to name a host, and inventing one would be
 * exactly the fabrication this task forbids. Hashing the text means re-importing the
 * same file replaces it instead of piling up duplicates.
 */
private fun fallbackProfileId(configText: String): String =
    com.kx.vpn.domain.servers.EndpointIdentity.profileId(
        host = "config",
        port = 0,
        publicKey = configText,
    )

internal class ServersViewModelFactory(
    private val repository: com.kx.vpn.data.EndpointCatalogueRepository?,
    private val probe: com.kx.vpn.platform.TcpLatencyProbe?,
    private val switchController: com.kx.vpn.data.EndpointSwitchController?,
    private val network: com.kx.vpn.platform.NetworkAvailability?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
        requireNotNull(
            com.kx.vpn.ui.servers.ServersViewModel(
                repository = repository
                    ?: throw IllegalStateException("no catalogue repository was supplied"),
                probe = probe ?: com.kx.vpn.platform.TcpLatencyProbe(),
                switchController = switchController,
                network = network,
            ) as? T,
        ) { "Requested $modelClass from a Servers-only factory" }
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
