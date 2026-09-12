package com.yourteam.deliveryagent.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.deliveryagent.data.model.Order
import com.yourteam.deliveryagent.data.model.OrderStatus
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.OrderRepository
import com.yourteam.deliveryagent.data.repository.TodayStats
import io.github.jan.supabase.realtime.RealtimeDataFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val supabase: SupabaseClient,
    private val orderRepository: OrderRepository,
    private val agentRepository: AgentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    init {
        loadDashboardData()
        subscribeToRealtimeUpdates()
    }

    // ── Initial data load ────────────────────────────────────────────────────

    private fun loadDashboardData() {
        viewModelScope.launch {
            val userId = currentUserId ?: run {
                _uiState.value = DashboardUiState.Error("Not authenticated")
                return@launch
            }

            // Load profile + agent data in parallel
            val profileAgentResult = agentRepository.fetchAgentProfile(userId)
            if (profileAgentResult.isFailure) {
                _uiState.value = DashboardUiState.Error(
                    profileAgentResult.exceptionOrNull()?.message ?: "Failed to load profile"
                )
                return@launch
            }

            val (profile, agent) = profileAgentResult.getOrNull()!!

            // Load other dashboard data in parallel
            val availableResult = orderRepository.fetchAvailableOrders()
            val activeResult    = orderRepository.fetchActiveOrder(agent.id)
            val statsResult     = agentRepository.fetchTodayStats(agent.id)

            if (availableResult.isFailure || activeResult.isFailure || statsResult.isFailure) {
                _uiState.value = DashboardUiState.Error(
                    availableResult.exceptionOrNull()?.message
                        ?: activeResult.exceptionOrNull()?.message
                        ?: statsResult.exceptionOrNull()?.message
                        ?: "Failed to load dashboard"
                )
                return@launch
            }

            val availableOrders = availableResult.getOrNull() ?: emptyList()
            val activeOrder     = activeResult.getOrNull()
            val todayStats      = statsResult.getOrNull() ?: TodayStats(0, 0.0)

            _uiState.value = DashboardUiState.Success(
                profile                  = profile,
                agent                    = agent,
                availableDeliveriesCount = availableOrders.size,
                activeOrder              = activeOrder,
                todayCompletedCount      = todayStats.completedCount,
                todayEarnings            = todayStats.earnings,
            )
        }
    }

    // ── Realtime subscriptions ───────────────────────────────────────────────

    /**
     * Subscribe to changes on the orders table to keep available count live.
     * Listens for INSERT/UPDATE/DELETE on orders with status=READY_FOR_PICKUP.
     */
    private fun subscribeToRealtimeUpdates() {
        viewModelScope.launch {
            try {
                val channel = supabase.channel("public:orders") {
                    // Subscribe to all changes on the orders table
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
     * Handle a Realtime event: update available count based on order status changes.
     */
    private suspend fun handleRealtimeEvent(event: RealtimeDataFlow.PostgresChangeEvent<Order>) {
        val currentState = _uiState.value
        if (currentState !is DashboardUiState.Success) return

        val order = event.record ?: return

        // Only care about READY_FOR_PICKUP orders that are unassigned
        val isAvailable = order.status == OrderStatus.READY_FOR_PICKUP.name && order.agentId == null

        when (event) {
            is RealtimeDataFlow.PostgresChangeEvent.Insert -> {
                // New order inserted
                if (isAvailable) {
                    _uiState.value = currentState.copy(
                        availableDeliveriesCount = currentState.availableDeliveriesCount + 1
                    )
                }
            }

            is RealtimeDataFlow.PostgresChangeEvent.Update -> {
                // Order updated — check if it transitioned from available to taken
                if (!isAvailable && order.agentId != null) {
                    // An agent just claimed an available order
                    if (currentState.availableDeliveriesCount > 0) {
                        _uiState.value = currentState.copy(
                            availableDeliveriesCount = currentState.availableDeliveriesCount - 1
                        )
                    }
                }

                // Also check if this is the current agent's active order
                if (order.agentId == currentState.agent.id) {
                    val activeStatuses = setOf(
                        OrderStatus.AGENT_ASSIGNED.name,
                        OrderStatus.PICKED_UP.name,
                        OrderStatus.OUT_FOR_DELIVERY.name,
                    )
                    if (order.status in activeStatuses) {
                        _uiState.value = currentState.copy(activeOrder = order)
                    } else if (order.status == OrderStatus.DELIVERED.name) {
                        // Order delivered — re-fetch today's stats
                        val statsResult = agentRepository.fetchTodayStats(currentState.agent.id)
                        statsResult.onSuccess { stats ->
                            _uiState.value = currentState.copy(
                                activeOrder         = null,
                                todayCompletedCount = stats.completedCount,
                                todayEarnings       = stats.earnings,
                            )
                        }
                    }
                }
            }

            is RealtimeDataFlow.PostgresChangeEvent.Delete -> {
                // Order deleted (unlikely in normal flow)
                if (isAvailable && currentState.availableDeliveriesCount > 0) {
                    _uiState.value = currentState.copy(
                        availableDeliveriesCount = currentState.availableDeliveriesCount - 1
                    )
                }
            }

            else -> {}
        }
    }

    // ── User interactions ────────────────────────────────────────────────────

    /**
     * Toggle agent availability (online ↔ offline).
     * Updates delivery_agents.is_online and reflects in UI.
     */
    fun toggleAvailability(newOnlineStatus: Boolean) {
        val currentState = _uiState.value
        if (currentState !is DashboardUiState.Success) return

        _uiState.value = currentState.copy(isTogglingAvailability = true)

        viewModelScope.launch {
            agentRepository.toggleOnlineStatus(currentState.agent.id, newOnlineStatus).fold(
                onSuccess = {
                    _uiState.value = currentState.copy(
                        agent = currentState.agent.copy(isOnline = newOnlineStatus),
                        isTogglingAvailability = false,
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        isTogglingAvailability = false,
                        lastUpdateError = "Failed to update availability: ${error.message}",
                    )
                },
            )
        }
    }

    /**
     * Manually refresh all dashboard data (e.g., on pull-to-refresh).
     */
    fun refreshDashboard() {
        _uiState.value = DashboardUiState.Loading
        loadDashboardData()
    }
}
