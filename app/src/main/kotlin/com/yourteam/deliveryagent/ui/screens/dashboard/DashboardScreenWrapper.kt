package com.yourteam.deliveryagent.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourteam.deliveryagent.DeliveryAgentApp
import com.yourteam.deliveryagent.ui.screens.HomeScreen

/**
 * Wrapper composable that creates the DashboardViewModel with proper dependency injection
 * and wires it to the HomeScreen.
 *
 * This is called from AppNavGraph → composable(NavRoutes.HOME).
 */
@Composable
fun DashboardScreenWrapper(
    onNavigateToAvailableDeliveries: () -> Unit = {},
    onNavigateToDeliveryDetails: (orderId: String) -> Unit = {},
) {
    val context = LocalContext.current
    val container = (context.applicationContext as DeliveryAgentApp).container

    // Create ViewModel using a factory
    val viewModel = viewModel<DashboardViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(
                    supabase       = com.yourteam.deliveryagent.core.SupabaseClientProvider.client,
                    orderRepository = container.orderRepository,
                    agentRepository = container.agentRepository,
                ) as T
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        uiState = uiState,
        onNavigateToAvailableDeliveries = onNavigateToAvailableDeliveries,
        onNavigateToDeliveryDetails     = onNavigateToDeliveryDetails,
        onToggleAvailability            = { isOnline -> viewModel.toggleAvailability(isOnline) },
        onRefresh                       = { viewModel.refreshDashboard() },
    )
}
