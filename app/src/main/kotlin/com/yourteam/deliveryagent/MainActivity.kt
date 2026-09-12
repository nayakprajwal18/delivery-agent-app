package com.yourteam.deliveryagent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yourteam.deliveryagent.navigation.AppNavGraph
import com.yourteam.deliveryagent.navigation.NavRoutes
import com.yourteam.deliveryagent.ui.theme.DeliveryAgentTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Determine start destination based on whether a Supabase session already exists.
        val container = (application as DeliveryAgentApp).container
        val startDestination = if (container.authRepository.hasActiveSession()) {
            NavRoutes.HOME
        } else {
            NavRoutes.LOGIN
        }

        setContent {
            DeliveryAgentTheme {
                AppNavGraph(startDestination = startDestination)
            }
        }
    }
}
