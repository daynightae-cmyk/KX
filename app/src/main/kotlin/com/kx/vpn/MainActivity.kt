package com.kx.vpn

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.ui.launch.LaunchUiState
import com.kx.vpn.ui.launch.LaunchViewModel
import com.kx.vpn.ui.navigation.KXNavHost
import com.kx.vpn.ui.theme.KXTheme

class MainActivity : ComponentActivity() {

    private val launchViewModel: LaunchViewModel by viewModels {
        val application = application as KXApplication
        LaunchViewModelFactory(application.launchStateRepository)
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

        setContent {
            // KXTheme follows the real system dark-mode setting. There is no in-app
            // theme switch yet — that belongs to TASK_10.
            KXTheme {
                val navController = rememberNavController()
                KXNavHost(
                    navController = navController,
                    launchViewModel = launchViewModel,
                    repository = (application as KXApplication).launchStateRepository,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
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
