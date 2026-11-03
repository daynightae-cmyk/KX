package com.knoux.kx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.knoux.kx.startup.PreferencesStartupRepository
import com.knoux.kx.startup.StartupViewModel
import com.knoux.kx.ui.KxApp
import com.knoux.kx.ui.KxTheme

class MainActivity : ComponentActivity() {
    private val startup: StartupViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(StartupViewModel::class.java))
                return StartupViewModel(PreferencesStartupRepository(applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KxTheme {
                val state = startup.state.collectAsStateWithLifecycle()
                KxApp(state.value, startup::retry)
            }
        }
    }
}
