package com.yourteam.deliveryagent.ui.screens.profile

import com.yourteam.deliveryagent.data.model.Profile
import com.yourteam.deliveryagent.data.model.DeliveryAgent

/**
 * UI state for the Profile screen.
 */
sealed class ProfileUiState {

    /** Initial load in progress. */
    object Loading : ProfileUiState()

    /** Profile and agent data loaded successfully. */
    data class Success(
        val profile: Profile,
        val agent: DeliveryAgent,
        val phone: String,
        val fullName: String = profile.fullName,
        val vehicleType: String = agent.vehicleType,
        val vehicleNumber: String = agent.vehicleNumber,
        val isAvailable: Boolean = agent.isOnline,
        val isSaving: Boolean = false,
        val saveError: String? = null,
    ) : ProfileUiState()

    /** Error loading profile. */
    data class Error(val message: String) : ProfileUiState()
}

/**
 * UI events emitted by this ViewModel (e.g., navigation).
 */
sealed class ProfileUiEvent {
    object NavigateToLogin : ProfileUiEvent()
}
