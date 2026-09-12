package com.yourteam.deliveryagent.ui.screens.deliverydetails

import com.yourteam.deliveryagent.data.model.Order

/**
 * UI state for the Delivery Details screen.
 */
sealed class DeliveryDetailsUiState {

    /** Initial load in progress. */
    object Loading : DeliveryDetailsUiState()

    /** Order loaded successfully. */
    data class Success(
        val order: Order,
        val isConfirmingPickup: Boolean = false,
        val isConfirmingDelivery: Boolean = false,
        val confirmationError: String? = null,
    ) : DeliveryDetailsUiState()

    /** Error loading order. */
    data class Error(val message: String) : DeliveryDetailsUiState()
}

/**
 * UI events emitted by this ViewModel (e.g., navigation).
 */
sealed class DeliveryDetailsUiEvent {
    object NavigateToHome : DeliveryDetailsUiEvent()
    object NavigateBack : DeliveryDetailsUiEvent()
}
