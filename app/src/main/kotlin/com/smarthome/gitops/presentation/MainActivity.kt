package com.smarthome.gitops.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smarthome.gitops.presentation.ui.SmartHomeApp
import com.smarthome.gitops.presentation.ui.theme.SmartHomeTheme

/**
 * MainActivity — View layer entry point.
 *
 * ── MVVM Compliance ──────────────────────────────────────────────────────────
 * This class is intentionally minimal. It contains ZERO business logic:
 *   • No network calls
 *   • No data parsing
 *   • No coroutines launched directly
 *   • No direct ViewModel interaction beyond delegating to the Compose tree
 *
 * All logic lives in MainViewModel and GitHubRepository.
 * All UI composition is handled by SmartHomeApp composable.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartHomeApp()
                }
            }
        }
    }
}
