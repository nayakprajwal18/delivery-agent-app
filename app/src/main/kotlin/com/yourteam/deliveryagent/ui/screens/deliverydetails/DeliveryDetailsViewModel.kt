package com.yourteam.deliveryagent.ui.screens.deliverydetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.deliveryagent.data.model.OrderStatus
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.OrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DeliveryDetailsViewModel(
    private val orderId: String,
    private val supabase: SupabaseClient,
    private val orderRepository: OrderRepository,
    private val agentRepository: AgentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeliveryDetailsUiState>(DeliveryDetailsUiState.Loading)
    val uiState: StateFlow<DeliveryDetailsUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<DeliveryDetailsUiEvent>()
    val uiEvents: SharedFlow<DeliveryDetailsUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadOrderDetails()
        subscribeToOrderUpdates()
    }

    // ── Initial data load ────────────────────────────────────────────────────

    private fun loadOrderDetails() {
        viewModelScope.launch {
            val result = orderRepository.fetchOrderById(orderId)
            result.fold(
                onSuccess = { order ->
                    _uiState.value = DeliveryDetailsUiState.Success(order = order)
                },
                onFailure = { error ->
                    _uiState.value = DeliveryDetailsUiState.Error(
                        error.message ?: "Failed to load order details"
                    )
                },
            )
        }
    }

    // ── Realtime subscription ────────────────────────────────────────────────

    /**
     * Subscribe to changes on this specific order so external status changes are reflected.
     */
    private fun subscribeToOrderUpdates() {
        viewModelScope.launch {
            try {
                val channel = supabase.channel("public:orders") {
                    postgresChangeFlow<com.yourteam.deliveryagent.data.model.Order>("public") {
                        table = "orders"
                    }
                }

                channel.postgresChangeFlow<com.yourteam.deliveryagent.data.model.Order>("public") {
                    table = "orders"
                }
                    .onEach { event ->
                        val order = event.record ?: return@onEach
                        if (order.id == orderId) {
                            // Update UI with the new order state
                            val currentState = _uiState.value
                            if (currentState is DeliveryDetailsUiState.Success) {
                                _uiState.value = currentState.copy(order = order)
                            }
                        }
                    }
                    .launchIn(viewModelScope)

                channel.subscribe()
            } catch (e: Exception) {
                // Realtime failure is not fatal; data still loaded on init
            }
        }
    }

    // ── Confirmation actions ─────────────────────────────────────────────────

    /**
     * Confirm pickup: update status to PICKED_UP.
     * Called when agent taps "Confirm Pickup" button.
     */
    fun confirmPickup() {
        val currentState = _uiState.value
        if (currentState !is DeliveryDetailsUiState.Success) return

        _uiState.value = currentState.copy(isConfirmingPickup = true)

        viewModelScope.launch {
            val result = orderRepository.updateOrderStatus(orderId, OrderStatus.PICKED_UP)
            result.fold(
                onSuccess = {
                    // Status update succeeded; Realtime will push the new state
                    _uiState.value = currentState.copy(isConfirmingPickup = false)
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isConfirmingPickup = false,
                        confirmationError   = "Failed to confirm pickup: ${error.message}",
                    )
                },
            )
        }
    }

    /**
     * Confirm delivery: update status to DELIVERED and increment agent's total_deliveries and total_earnings.
     * Called when agent taps "Confirm Delivery" button.
     */
    fun confirmDelivery() {
        val currentState = _uiState.value
        if (currentState !is DeliveryDetailsUiState.Success) return

        val order = currentState.order
        val agentId = order.agentId ?: run {
            _uiState.value = currentState.copy(
                confirmationError = "Agent ID not found"
            )
            return
        }

        _uiState.value = currentState.copy(isConfirmingDelivery = true)

        viewModelScope.launch {
            val result = orderRepository.confirmDeliveryAndUpdateAgent(orderId, agentId)
            result.fold(
                onSuccess = { updatedOrder ->
                    // Status update succeeded; navigate back to Home
                    _uiState.value = currentState.copy(
                        isConfirmingDelivery = false,
                        order = updatedOrder,
                    )
                    _uiEvents.emit(DeliveryDetailsUiEvent.NavigateToHome)
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isConfirmingDelivery = false,
                        confirmationError     = "Failed to confirm delivery: ${error.message}",
                    )
                },
            )
        }
    }

    /**
     * Dismiss the confirmation error message.
     */
    fun dismissConfirmationError() {
        val currentState = _uiState.value
        if (currentState is DeliveryDetailsUiState.Success) {
            _uiState.value = currentState.copy(confirmationError = null)
        }
    }
}
