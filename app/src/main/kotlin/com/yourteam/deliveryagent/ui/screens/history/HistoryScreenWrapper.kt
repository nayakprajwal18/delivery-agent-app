package com.yourteam.deliveryagent.ui.screens.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.yourteam.deliveryagent.core.SupabaseClientProvider
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.AuthRepository
import com.yourteam.deliveryagent.data.repository.OrderRepository
import com.yourteam.deliveryagent.ui.screens.HistoryScreen

/**
 * Wrapper composable that instantiates [HistoryViewModel] with DI,
 * collects UI state, and renders [HistoryScreen].
 */
@Composable
fun HistoryScreenWrapper() {
    // Instantiate repositories with DI
    val supabase = SupabaseClientProvider.client
    val authRepository = AuthRepository(supabase)
    val agentRepository = AgentRepository(supabase)
    val orderRepository = OrderRepository(supabase)

    val viewModel = remember {
        HistoryViewModel(
            authRepository = authRepository,
            agentRepository = agentRepository,
            orderRepository = orderRepository,
        )
    }

    val uiState = viewModel.uiState.collectAsState().value

    HistoryScreen(
        uiState = uiState,
        onSelectOrder = { orderId -> viewModel.selectOrder(orderId) },
        onDismissOrderDetails = { viewModel.dismissOrderDetails() },
        onRetry = { viewModel.retry() },
    )
}
