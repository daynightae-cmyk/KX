package com.kx.vpn

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.platform.KXVpnService
import com.kx.vpn.ui.launch.LaunchUiState
import com.kx.vpn.ui.launch.LaunchViewModel
import com.kx.vpn.ui.navigation.KXNavHost
import com.kx.vpn.ui.theme.KXTheme

class MainActivity : ComponentActivity() {

    private val kxApplication: KXApplication
        get() = application as KXApplication

    private val sessionManager
        get() = kxApplication.tunnelSessionManager

    private val launchViewModel: LaunchViewModel by viewModels {
        LaunchViewModelFactory(kxApplication.launchStateRepository)
    }

    /**
     * The platform's VPN consent sheet. It is only ever launched from a connect
     * action — never automatically at onboarding — which is what the security model
     * asks for.
     */
    private val consentLauncher: ActivityResultLauncher<android.content.Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                sessionManager.onPermissionGranted()
            } else {
                sessionManager.onPermissionDenied()
            }
        }

    /**
     * Android 13+ needs POST_NOTIFICATIONS for the ongoing tunnel notification to be
     * visible. It is requested when the user connects, not at onboarding, and the
     * connection proceeds either way — a denied notification permission must not stop
     * a tunnel the user asked for.
     */
    private val notificationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            sessionManager.connect()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Installed before super.onCreate() — that ordering is what lets the platform
        // draw the Android 12+ splash instead of a blank window.
        val splashDeadline = SystemClock.elapsedRealtime() + MaxSplashHoldMillis
        installSplashScreen().setKeepOnScreenCondition {
            // The condition is bounded: a wedged store must not strand the user on the
            // system splash forever.
            launchViewModel.state.value is LaunchUiState.Loading &&
                SystemClock.elapsedRealtime() < splashDeadline
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager.onConsentRequired = {
            val request = KXVpnService.prepareConsent(this)
            if (request == null) {
                sessionManager.onPermissionGranted()
            } else {
                consentLauncher.launch(request)
            }
        }

        setContent {
            // KXTheme follows the real system dark-mode setting. There is no in-app
            // theme switch yet — that belongs to TASK_10.
            KXTheme {
                val navController = rememberNavController()
                KXNavHost(
                    navController = navController,
                    launchViewModel = launchViewModel,
                    repository = kxApplication.launchStateRepository,
                    sessionManager = sessionManager,
                    profileRepository = kxApplication.profileRepository,
                    onConnect = ::requestConnect,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onDestroy() {
        // The manager is process-scoped, so the activity must not stay referenced by it.
        sessionManager.onConsentRequired = null
        super.onDestroy()
    }

    /**
     * Asks for the notification permission when the platform requires it, then starts
     * the connect flow regardless of the answer.
     */
    private fun requestConnect() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        sessionManager.connect()
    }

    private companion object {
        /** Hard cap on how long the platform splash may be held. */
        const val MaxSplashHoldMillis = 3_000L
    }
}

internal class LaunchViewModelFactory(
    private val repository: LaunchStateRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        requireNotNull(LaunchViewModel(repository) as? T) {
            "Requested $modelClass from a LaunchViewModel-only factory"
        }
}
