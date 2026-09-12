package com.yourteam.deliveryagent.ui.screens.deliverydetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.yourteam.deliveryagent.core.SupabaseClientProvider
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.OrderRepository
import com.yourteam.deliveryagent.ui.screens.DeliveryDetailsScreen

/**
 * Wrapper composable that instantiates [DeliveryDetailsViewModel] with DI,
 * collects UI state, and renders [DeliveryDetailsScreen].
 */
@Composable
fun DeliveryDetailsScreenWrapper(
    orderId: String,
    onNavigateBack: () -> Unit = {},
    onDeliveryCompleted: () -> Unit = {},
) {
    // Instantiate ViewModel with DI
    val supabase = SupabaseClientProvider.client
    val orderRepository = OrderRepository(supabase)
    val agentRepository = AgentRepository(supabase)
    
    val viewModel = remember {
        DeliveryDetailsViewModel(
            orderId = orderId,
            supabase = supabase,
            orderRepository = orderRepository,
            agentRepository = agentRepository,
        )
    }
    
    val uiState = viewModel.uiState.collectAsState().value

    // Observe UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                DeliveryDetailsUiEvent.NavigateToHome -> onDeliveryCompleted()
                DeliveryDetailsUiEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    DeliveryDetailsScreen(
        orderId = orderId,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDeliveryCompleted = onDeliveryCompleted,
        onConfirmPickup = { viewModel.confirmPickup() },
        onConfirmDelivery = { viewModel.confirmDelivery() },
        onDismissError = { viewModel.dismissConfirmationError() },
    )
}
