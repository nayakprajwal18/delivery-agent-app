package com.yourteam.deliveryagent.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourteam.deliveryagent.core.FCMTokenManager
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val agentRepository: AgentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ── Phone validation & formatting ────────────────────────────────────────

    /**
     * Formats a phone number for Indian format (10-digit).
     * Returns "+91" + digits if valid, otherwise null.
     *
     * Handles: "1234567890", "91 1234567890", "+91 1234567890", etc.
     */
    fun formatPhoneForSubmission(input: String): String? {
        val cleaned = input.replace(Regex("[\\s+-]"), "")
        val digits = cleaned.filter { it.isDigit() }

        // If the user entered 10 digits, it's local format → prepend +91
        if (digits.length == 10) {
            return "+91$digits"
        }

        // If the user entered 12 digits starting with 91, it's +91 format
        if (digits.length == 12 && digits.startsWith("91")) {
            return "+91${digits.drop(2)}"
        }

        return null
    }

    /**
     * Display-friendly format: show only the last 10 digits (local format) to the user.
     */
    fun formatPhoneForDisplay(phone: String?): String {
        if (phone == null) return ""
        val digits = phone.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    // ── Auth flow ────────────────────────────────────────────────────────────

    /**
     * Step 1: User enters phone number and taps "Send OTP".
     * Validates the phone, calls Supabase to send OTP, and transitions to OtpSent state.
     */
    fun sendOtp(phoneInput: String) {
        val formattedPhone = formatPhoneForSubmission(phoneInput)
        if (formattedPhone == null) {
            _uiState.value = LoginUiState.Error(
                message       = "Please enter a valid 10-digit mobile number",
                currentPhase  = LoginUiState.Error.Phase.SENDING_OTP,
            )
            return
        }

        _uiState.value = LoginUiState.SendingOtp

        viewModelScope.launch {
            authRepository.sendOtp(formattedPhone).fold(
                onSuccess = {
                    _uiState.value = LoginUiState.OtpSent(phone = formattedPhone, resendCooldownSeconds = 30)
                    // Start the 30-second cooldown timer.
                    startResendCooldown(formattedPhone)
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        message       = "Failed to send OTP: ${error.message ?: "Unknown error"}",
                        currentPhase  = LoginUiState.Error.Phase.SENDING_OTP,
                    )
                },
            )
        }
    }

    /**
     * Step 2: User enters the 6-digit OTP and taps "Verify".
     * Calls Supabase to verify the OTP, then creates/checks the profile and agent row.
     */
    fun verifyOtp(otp: String) {
        val currentState = _uiState.value
        if (currentState !is LoginUiState.OtpSent) return

        _uiState.value = LoginUiState.VerifyingOtp

        viewModelScope.launch {
            authRepository.verifyOtp(
                phone = currentState.phone,
                otp   = otp,
            ).fold(
                onSuccess = {
                    // OTP verified — now check/create profile and agent row.
                    ensureAgentProfile()
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        message       = "Invalid or expired OTP: ${error.message ?: "Unknown error"}",
                        currentPhase  = LoginUiState.Error.Phase.VERIFYING_OTP,
                    )
                },
            )
        }
    }

    /**
     * Step 3: After successful OTP verification, ensure the user has a delivery_agent profile.
     * If not, create one (both profiles and delivery_agents rows).
     * Then retrieve and save the FCM token for push notifications.
     */
    private suspend fun ensureAgentProfile() {
        authRepository.ensureDeliveryAgentProfile().fold(
            onSuccess = {
                // Profile created/verified — now get and save FCM token
                saveFcmToken()
            },
            onFailure = { error ->
                _uiState.value = LoginUiState.Error(
                    message       = "Failed to set up profile: ${error.message ?: "Unknown error"}",
                    currentPhase  = LoginUiState.Error.Phase.CREATING_PROFILE,
                )
            },
        )
    }

    /**
     * Retrieves the FCM token and saves it to the delivery_agents row for the current user.
     * This allows the backend to send targeted push notifications to this device.
     *
     * On error, we still transition to VerificationSuccess so the user isn't blocked,
     * but log the failure for debugging.
     */
    private suspend fun saveFcmToken() {
        val agentId = authRepository.currentUserId() ?: run {
            // This shouldn't happen, but handle gracefully
            _uiState.value = LoginUiState.VerificationSuccess
            return
        }

        val token = FCMTokenManager.getToken()
        if (token != null) {
            agentRepository.saveFcmToken(agentId, token).fold(
                onSuccess = {
                    _uiState.value = LoginUiState.VerificationSuccess
                },
                onFailure = { error ->
                    // Log the failure but still allow login to succeed
                    android.util.Log.e("LoginViewModel", "Failed to save FCM token: ${error.message}")
                    _uiState.value = LoginUiState.VerificationSuccess
                },
            )
        } else {
            // Token retrieval failed, but still allow login
            android.util.Log.w("LoginViewModel", "FCM token is null, skipping save")
            _uiState.value = LoginUiState.VerificationSuccess
        }
    }

    /**
     * Countdown timer for the "Resend OTP" button.
     * Updates the resend cooldown every second until it reaches 0.
     */
    private fun startResendCooldown(phone: String) {
        viewModelScope.launch {
            var seconds = 30
            while (seconds > 0) {
                delay(1000)
                seconds--
                val currentState = _uiState.value
                if (currentState is LoginUiState.OtpSent) {
                    _uiState.value = currentState.copy(resendCooldownSeconds = seconds)
                }
            }
        }
    }

    /**
     * User tapped "Resend OTP" (only available when cooldown is 0).
     * Re-send the OTP and reset the cooldown.
     */
    fun resendOtp() {
        val currentState = _uiState.value
        if (currentState !is LoginUiState.OtpSent) return

        sendOtp(formatPhoneForDisplay(currentState.phone))
    }

    /**
     * User dismissed an error — return to the appropriate input state.
     */
    fun dismissError() {
        val currentState = _uiState.value
        if (currentState is LoginUiState.Error) {
            when (currentState.currentPhase) {
                LoginUiState.Error.Phase.SENDING_OTP -> {
                    _uiState.value = LoginUiState.Idle
                }
                LoginUiState.Error.Phase.VERIFYING_OTP -> {
                    // Stay on OTP screen so user can retry.
                    _uiState.value = LoginUiState.OtpSent(phone = "", resendCooldownSeconds = 30)
                }
                LoginUiState.Error.Phase.CREATING_PROFILE -> {
                    // Retry profile creation or fall back to phone screen.
                    _uiState.value = LoginUiState.Idle
                }
            }
        }
    }
}
