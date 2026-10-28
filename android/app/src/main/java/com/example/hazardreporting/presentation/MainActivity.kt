package com.example.hazardreporting.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hazardreporting.HazardReportingApp
import com.example.hazardreporting.presentation.navigation.HazardReportingNavHost
import com.example.hazardreporting.presentation.theme.HazardReportingTheme

/**
 * The app's single activity. Keeps the splash screen up until the start screen is decided,
 * then draws the navigation graph edge-to-edge
 *
 * @author Roman Pliskin on 29.10.2026
 */
class MainActivity : ComponentActivity() {

    private val viewModelFactory: ViewModelProvider.Factory by lazy {
        (application as HazardReportingApp).appComponent.viewModelFactory()
    }

    private val viewModel: MainViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.startDestination == null }
        enableEdgeToEdge()

        setContent {
            HazardReportingTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                uiState.startDestination?.let { startDestination ->
                    HazardReportingNavHost(
                        startDestination = startDestination,
                        viewModelFactory = viewModelFactory,
                    )
                }
            }
        }
    }
}
