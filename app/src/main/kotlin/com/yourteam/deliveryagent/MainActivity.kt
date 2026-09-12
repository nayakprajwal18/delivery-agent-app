package com.yourteam.deliveryagent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yourteam.deliveryagent.fcm.DeliveryFCMService
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

        // Check if this Activity was opened from an FCM notification tap.
        // The FCMService passes the deeplink route via EXTRA_DEEPLINK_ROUTE.
        val deepLinkRoute = intent.getStringExtra(DeliveryFCMService.EXTRA_DEEPLINK_ROUTE)
        if (deepLinkRoute != null) {
            Log.d("MainActivity", "Opened from FCM notification: $deepLinkRoute")
        }

        setContent {
            DeliveryAgentTheme {
                AppNavGraph(
                    startDestination = startDestination,
                    initialDeepLinkRoute = deepLinkRoute,
                )
            }
        }
    }
}
