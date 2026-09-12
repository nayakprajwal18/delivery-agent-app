package com.yourteam.deliveryagent.ui.screens.availabledeliveries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.deliveryagent.data.model.Order
import com.yourteam.deliveryagent.data.model.OrderStatus
import com.yourteam.deliveryagent.data.repository.OrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeDataFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * UI events emitted by this ViewModel (e.g., navigation, snackbars).
 */
sealed class AvailableDeliveriesUiEvent {
    data class NavigateToDetails(val orderId: String) : AvailableDeliveriesUiEvent()
    data class ShowToast(val message: String) : AvailableDeliveriesUiEvent()
}

class AvailableDeliveriesViewModel(
    private val supabase: SupabaseClient,
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AvailableDeliveriesUiState>(AvailableDeliveriesUiState.Loading)
    val uiState: StateFlow<AvailableDeliveriesUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<AvailableDeliveriesUiEvent>()
    val uiEvents: SharedFlow<AvailableDeliveriesUiEvent> = _uiEvents.asSharedFlow()

    private val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    init {
        loadAvailableDeliveries()
        subscribeToRealtimeUpdates()
    }

    // ── Initial data load ────────────────────────────────────────────────────

    private fun loadAvailableDeliveries() {
        viewModelScope.launch {
            val result = orderRepository.fetchAvailableOrders()
            result.fold(
                onSuccess = { orders ->
                    _uiState.value = if (orders.isEmpty()) {
                        AvailableDeliveriesUiState.Empty
                    } else {
                        AvailableDeliveriesUiState.Success(orders = orders)
                    }
                },
                onFailure = { error ->
                    _uiState.value = AvailableDeliveriesUiState.Error(
                        error.message ?: "Failed to load available deliveries"
                    )
                },
            )
        }
    }

    // ── Realtime subscriptions ───────────────────────────────────────────────

    /**
     * Subscribe to changes on the orders table to keep the list live.
     * Listens for INSERT/UPDATE/DELETE on orders with status=READY_FOR_PICKUP.
     */
    private fun subscribeToRealtimeUpdates() {
        viewModelScope.launch {
            try {
                val channel = supabase.channel("public:orders") {
                    postgresChangeFlow<Order>("public") {
                        table = "orders"
                    }
                }

                channel.postgresChangeFlow<Order>("public") {
                    table = "orders"
                }
                    .onEach { event ->
                        handleRealtimeEvent(event)
                    }
                    .launchIn(viewModelScope)

                channel.subscribe()
            } catch (e: Exception) {
                // Realtime subscription failure is not fatal; data will still load on refresh.
            }
        }
    }

    /**
     * Handle a Realtime event: add/remove available orders from the list.
     */
    private suspend fun handleRealtimeEvent(event: RealtimeDataFlow.PostgresChangeEvent<Order>) {
        val currentState = _uiState.value
        val order = event.record ?: return

        // Only care about READY_FOR_PICKUP orders that are unassigned
        val isAvailable = order.status == OrderStatus.READY_FOR_PICKUP.name && order.agentId == null

        when (event) {
            is RealtimeDataFlow.PostgresChangeEvent.Insert -> {
                // New order inserted and is available
                if (isAvailable && currentState is AvailableDeliveriesUiState.Success) {
                    val updatedOrders = listOf(order) + currentState.orders
                    _uiState.value = currentState.copy(orders = updatedOrders)
                } else if (isAvailable && currentState is AvailableDeliveriesUiState.Empty) {
                    _uiState.value = AvailableDeliveriesUiState.Success(orders = listOf(order))
                }
            }

            is RealtimeDataFlow.PostgresChangeEvent.Update -> {
                // Order updated — check if it was claimed by someone else
                if (!isAvailable && currentState is AvailableDeliveriesUiState.Success) {
                    // Remove this order from the list (it was taken)
                    val updatedOrders = currentState.orders.filterNot { it.id == order.id }
                    _uiState.value = if (updatedOrders.isEmpty()) {
                        AvailableDeliveriesUiState.Empty
                    } else {
                        currentState.copy(orders = updatedOrders)
                    }
                }
            }

            is RealtimeDataFlow.PostgresChangeEvent.Delete -> {
                // Order deleted (unlikely in normal flow)
                if (currentState is AvailableDeliveriesUiState.Success) {
                    val updatedOrders = currentState.orders.filterNot { it.id == order.id }
                    _uiState.value = if (updatedOrders.isEmpty()) {
                        AvailableDeliveriesUiState.Empty
                    } else {
                        currentState.copy(orders = updatedOrders)
                    }
                }
            }

            else -> {}
        }
    }

    // ── User interactions ────────────────────────────────────────────────────

    /**
     * Accept a delivery: atomically set agent_id and status to AGENT_ASSIGNED,
     * but only if agent_id is still null (prevent two agents from accepting the same order).
     */
    fun acceptOrder(orderId: String) {
        val agentId = currentUserId ?: return
        val currentState = _uiState.value
        if (currentState !is AvailableDeliveriesUiState.Success) return

        // Show loading spinner on this order
        _uiState.value = currentState.copy(acceptingOrderId = orderId)

        viewModelScope.launch {
            val result = orderRepository.acceptOrder(orderId, agentId)
            result.fold(
                onSuccess = {
                    // Navigate to Delivery Details for the newly accepted order
                    _uiEvents.emit(AvailableDeliveriesUiEvent.NavigateToDetails(orderId))

                    // Remove the order from the list
                    val updatedOrders = currentState.orders.filterNot { it.id == orderId }
                    _uiState.value = if (updatedOrders.isEmpty()) {
                        AvailableDeliveriesUiState.Empty
                    } else {
                        currentState.copy(
                            orders           = updatedOrders,
                            acceptingOrderId = null,
                        )
                    }
                },
                onFailure = { error ->
                    // Order was likely already claimed by another agent
                    _uiState.value = currentState.copy(
                        acceptingOrderId = null,
                        lastAcceptError  = "Already accepted by another agent",
                    )
                    _uiEvents.emit(
                        AvailableDeliveriesUiEvent.ShowToast("This delivery was just taken. Try another!")
                    )

                    // Refresh the list to see current state
                    loadAvailableDeliveries()
                },
            )
        }
    }

    /**
     * Manually refresh available deliveries list.
     */
    fun refreshDeliveries() {
        _uiState.value = AvailableDeliveriesUiState.Loading
        loadAvailableDeliveries()
    }

    /**
     * Dismiss the last accept error.
     */
    fun dismissAcceptError() {
        val currentState = _uiState.value
        if (currentState is AvailableDeliveriesUiState.Success) {
            _uiState.value = currentState.copy(lastAcceptError = null)
        }
    }
}
