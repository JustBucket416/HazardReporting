package com.example.hazardreporting.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.hazardreporting.presentation.theme.HazardReportingTheme

/**
 * The app's single activity. Draws edge-to-edge and hosts the Compose UI
 *
 * @author Roman Pliskin on 29.10.2026
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HazardReportingTheme {
                Surface(modifier = Modifier.fillMaxSize()) {}
            }
        }
    }
}
