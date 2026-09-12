package com.yourteam.deliveryagent.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.AuthRepository
import com.yourteam.deliveryagent.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val authRepository: AuthRepository,
    private val agentRepository: AgentRepository,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<HistoryUiEvent>()
    val uiEvents: SharedFlow<HistoryUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadDeliveryHistory()
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    /**
     * Load completed delivery orders for the current agent.
     * Fetches all DELIVERED orders ordered by delivered_at descending.
     */
    private fun loadDeliveryHistory() {
        viewModelScope.launch {
            try {
                // Get current user ID
                val userId = authRepository.currentUserId()
                    ?: throw IllegalStateException("No authenticated user")

                // Get the agent ID for this user
                val (_, agent) = agentRepository.fetchAgentProfile(userId).getOrThrow()

                // Fetch completed orders for this agent
                val orders = orderRepository.fetchCompletedOrders(agent.id).getOrThrow()

                _uiState.value = HistoryUiState.Success(orders = orders)
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error(
                    message = e.message ?: "Failed to load delivery history"
                )
            }
        }
    }

    // ── UI interactions ──────────────────────────────────────────────────────

    /**
     * Select or deselect an order for detail view.
     */
    fun selectOrder(orderId: String?) {
        val currentState = _uiState.value
        if (currentState is HistoryUiState.Success) {
            _uiState.value = currentState.copy(selectedOrderId = orderId)
        }
    }

    /**
     * Dismiss the order detail modal.
     */
    fun dismissOrderDetails() {
        val currentState = _uiState.value
        if (currentState is HistoryUiState.Success) {
            _uiState.value = currentState.copy(selectedOrderId = null)
        }
    }

    /**
     * Retry loading the history (e.g., after an error).
     */
    fun retry() {
        loadDeliveryHistory()
    }
}
