package com.yourteam.deliveryagent.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val agentRepository: AgentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<ProfileUiEvent>()
    val uiEvents: SharedFlow<ProfileUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadProfile()
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    /**
     * Load current user's profile and agent information.
     */
    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId()
                    ?: throw IllegalStateException("No authenticated user")

                val phone = authRepository.currentUserPhone()
                    ?: throw IllegalStateException("No phone for authenticated user")

                val (profile, agent) = agentRepository.fetchAgentProfile(userId).getOrThrow()

                _uiState.value = ProfileUiState.Success(
                    profile = profile,
                    agent = agent,
                    phone = phone,
                    fullName = profile.fullName,
                    vehicleType = agent.vehicleType,
                    vehicleNumber = agent.vehicleNumber,
                    isAvailable = agent.isOnline,
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    message = e.message ?: "Failed to load profile"
                )
            }
        }
    }

    // ── Field updates ────────────────────────────────────────────────────────

    /**
     * Update the full name field.
     */
    fun updateFullName(name: String) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            _uiState.value = currentState.copy(fullName = name)
        }
    }

    /**
     * Update the vehicle type field.
     */
    fun updateVehicleType(type: String) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            _uiState.value = currentState.copy(vehicleType = type)
        }
    }

    /**
     * Update the vehicle number field.
     */
    fun updateVehicleNumber(number: String) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            _uiState.value = currentState.copy(vehicleNumber = number)
        }
    }

    /**
     * Toggle the availability status.
     */
    fun toggleAvailability(isAvailable: Boolean) {
        val currentState = _uiState.value
        if (currentState is ProfileUiState.Success) {
            _uiState.value = currentState.copy(isAvailable = isAvailable)
        }
    }

    // ── Save changes ─────────────────────────────────────────────────────────

    /**
     * Save profile and agent changes to Supabase.
     * Updates profiles row (full_name) and delivery_agents row (vehicle_type, vehicle_number, is_online).
     */
    fun saveProfile() {
        val currentState = _uiState.value
        if (currentState !is ProfileUiState.Success) return

        _uiState.value = currentState.copy(isSaving = true, saveError = null)

        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId()
                    ?: throw IllegalStateException("No authenticated user")

                // Update profiles row
                agentRepository.updateProfile(
                    userId = userId,
                    fullName = currentState.fullName,
                ).getOrThrow()

                // Update delivery_agents row
                agentRepository.updateAgent(
                    agentId = currentState.agent.id,
                    vehicleType = currentState.vehicleType,
                    vehicleNumber = currentState.vehicleNumber,
                    isOnline = currentState.isAvailable,
                ).getOrThrow()

                // Success — update state without error
                _uiState.value = currentState.copy(
                    isSaving = false,
                    saveError = null,
                    // Update the profile and agent with new values
                    profile = currentState.profile.copy(fullName = currentState.fullName),
                    agent = currentState.agent.copy(
                        vehicleType = currentState.vehicleType,
                        vehicleNumber = currentState.vehicleNumber,
                        isOnline = currentState.isAvailable,
                    ),
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isSaving = false,
                    saveError = e.message ?: "Failed to save profile",
                )
            }
        }
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    /**
     * Sign out from Supabase and navigate to Login screen.
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.signOut().getOrThrow()
                _uiEvents.emit(ProfileUiEvent.NavigateToLogin)
            } catch (e: Exception) {
                // Even if signOut fails, navigate to login
                _uiEvents.emit(ProfileUiEvent.NavigateToLogin)
            }
        }
    }

    /**
     * Retry loading the profile (e.g., after an error).
     */
    fun retry() {
        loadProfile()
    }
}
