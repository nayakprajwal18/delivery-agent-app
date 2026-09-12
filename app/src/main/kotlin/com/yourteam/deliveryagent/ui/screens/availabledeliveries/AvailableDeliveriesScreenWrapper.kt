package com.yourteam.deliveryagent.ui.screens.availabledeliveries

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourteam.deliveryagent.DeliveryAgentApp
import com.yourteam.deliveryagent.core.SupabaseClientProvider
import com.yourteam.deliveryagent.ui.screens.AvailableDeliveriesScreen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Wrapper composable that creates the AvailableDeliveriesViewModel with proper DI
 * and wires it to the AvailableDeliveriesScreen, handling UI events.
 */
@Composable
fun AvailableDeliveriesScreenWrapper(
    onNavigateToDeliveryDetails: (orderId: String) -> Unit = {},
) {
    val context = LocalContext.current
    val container = (context.applicationContext as DeliveryAgentApp).container

    // Create ViewModel using a factory
    val viewModel = viewModel<AvailableDeliveriesViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AvailableDeliveriesViewModel(
                    supabase       = SupabaseClientProvider.client,
                    orderRepository = container.orderRepository,
                ) as T
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    // Handle UI events (navigation, toasts)
    LaunchedEffect(viewModel) {
        viewModel.uiEvents
            .onEach { event ->
                when (event) {
                    is AvailableDeliveriesUiEvent.NavigateToDetails -> {
                        onNavigateToDeliveryDetails(event.orderId)
                    }
                    is AvailableDeliveriesUiEvent.ShowToast -> {
                        // In a real app, show a Toast or Snackbar
                        // For now, it's handled by the screen's snackbar state
                    }
                }
            }
            .launchIn(viewModel.viewModelScope)
    }

    AvailableDeliveriesScreen(
        uiState                     = uiState,
        onNavigateToDeliveryDetails = onNavigateToDeliveryDetails,
        onAcceptOrder               = { orderId -> viewModel.acceptOrder(orderId) },
        onRefresh                   = { viewModel.refreshDeliveries() },
    )
}
