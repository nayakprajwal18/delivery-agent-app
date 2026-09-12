package com.yourteam.deliveryagent.ui.screens.login

/**
 * Sealed hierarchy of UI states for the Login screen.
 * Each state represents a distinct phase of the phone + OTP auth flow.
 */
sealed class LoginUiState {

    /** Initial state: phone number input field visible. */
    object Idle : LoginUiState()

    /** OTP sent, waiting for user to enter it. */
    data class OtpSent(
        val phone: String,
        val resendCooldownSeconds: Int = 30,
    ) : LoginUiState()

    /** Sending OTP to phone number — show spinner. */
    object SendingOtp : LoginUiState()

    /** Verifying OTP entered by user — show spinner. */
    object VerifyingOtp : LoginUiState()

    /** OTP verified and user profile/agent created (or already exists). Ready to proceed. */
    object VerificationSuccess : LoginUiState()

    /** Error state — display error message to user. */
    data class Error(
        val message: String,
        val currentPhase: Phase = Phase.SENDING_OTP,  // where the error occurred
    ) : LoginUiState() {
        enum class Phase {
            SENDING_OTP,
            VERIFYING_OTP,
            CREATING_PROFILE,
        }
    }
}
