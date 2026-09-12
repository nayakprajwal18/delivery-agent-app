package com.yourteam.deliveryagent.ui.screens.dashboard

import com.yourteam.deliveryagent.data.model.DeliveryAgent
import com.yourteam.deliveryagent.data.model.Order
import com.yourteam.deliveryagent.data.model.Profile

/**
 * UI state for the Home Dashboard screen.
 */
sealed class DashboardUiState {

    /** Initial load in progress. */
    object Loading : DashboardUiState()

    /** Data loaded successfully. */
    data class Success(
        val profile: Profile,
        val agent: DeliveryAgent,
        val availableDeliveriesCount: Int = 0,
        val activeOrder: Order? = null,
        val todayCompletedCount: Int = 0,
        val todayEarnings: Double = 0.0,
        val isTogglingAvailability: Boolean = false,
        val lastUpdateError: String? = null,
    ) : DashboardUiState()

    /** Error loading dashboard data. */
    data class Error(val message: String) : DashboardUiState()
}
