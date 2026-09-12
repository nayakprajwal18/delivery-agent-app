package com.yourteam.deliveryagent.ui.screens.history

import com.yourteam.deliveryagent.data.model.Order

/**
 * UI state for the History screen.
 */
sealed class HistoryUiState {

    /** Initial load in progress. */
    object Loading : HistoryUiState()

    /** Orders loaded successfully. */
    data class Success(
        val orders: List<Order> = emptyList(),
        val selectedOrderId: String? = null,  // For modal details view
    ) : HistoryUiState()

    /** Error loading orders. */
    data class Error(val message: String) : HistoryUiState()
}

/**
 * UI events emitted by this ViewModel (e.g., navigation).
 */
sealed class HistoryUiEvent {
    object NavigateBack : HistoryUiEvent()
}
