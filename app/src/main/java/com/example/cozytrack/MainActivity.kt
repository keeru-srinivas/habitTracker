package com.example.cozytrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.navigation.compose.rememberNavController
import com.example.cozytrack.presentation.navigation.AppNavHost
import com.example.cozytrack.presentation.navigation.Routes
import com.example.cozytrack.ui.theme.CozyTrackTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as CozyTrackApplication).appContainer

        setContent {
            CozyTrackTheme {
                val navController = rememberNavController()
                val startDestination by produceState<String?>(initialValue = null) {
                    value = if (appContainer.sessionManager.accessToken.first().isNullOrBlank()) {
                        Routes.LOGIN
                    } else {
                        Routes.HABITS
                    }
                }

                val destination = startDestination
                if (destination != null) {
                    AppNavHost(
                        navController = navController,
                        startDestination = destination,
                        appContainer = appContainer
                    )
                }
            }
        }
    }
}