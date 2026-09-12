package com.yourteam.deliveryagent.ui.screens.availabledeliveries

import com.yourteam.deliveryagent.data.model.Order

/**
 * UI state for the Available Deliveries screen.
 */
sealed class AvailableDeliveriesUiState {

    /** Initial load in progress. */
    object Loading : AvailableDeliveriesUiState()

    /** Deliveries loaded successfully. */
    data class Success(
        val orders: List<Order> = emptyList(),
        val acceptingOrderId: String? = null,  // order being accepted (show spinner)
        val lastAcceptError: String? = null,    // error message from accept attempt
    ) : AvailableDeliveriesUiState()

    /** Empty state — no available deliveries. */
    object Empty : AvailableDeliveriesUiState()

    /** Error loading deliveries. */
    data class Error(val message: String) : AvailableDeliveriesUiState()
}
