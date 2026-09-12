package com.yourteam.deliveryagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourteam.deliveryagent.ui.theme.DeliveryAgentTheme
import kotlinx.coroutines.launch

/**
 * Two-step login UI using phone + OTP via Supabase Auth.
 *
 * Step 1: User enters phone number (10 digits), taps "Send OTP"
 * Step 2: User enters 6-digit OTP, taps "Verify"
 *
 * On successful verification, the profile and delivery_agent rows are created
 * if they don't exist, and [onLoginSuccess] is called.
 */
@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onSendOtp: (phoneInput: String) -> Unit = {},
    onVerifyOtp: (otp: String) -> Unit = {},
    onResendOtp: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onLoginSuccess: () -> Unit = {},
) {
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var otpInput   by rememberSaveable { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Show error snackbar when an error occurs
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Error) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = uiState.message,
                    duration = androidx.compose.material3.SnackbarDuration.Long,
                )
            }
        }

        if (uiState is LoginUiState.VerificationSuccess) {
            onLoginSuccess()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Header ────────────────────────────────────────────────────
            Text(
                text  = "Delivery Agent",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text  = "Login with phone + OTP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(40.dp))

            // ── Step 1: Phone input ───────────────────────────────────────
            if (uiState is LoginUiState.Idle || 
                uiState is LoginUiState.SendingOtp ||
                uiState is LoginUiState.Error && uiState.currentPhase == LoginUiState.Error.Phase.SENDING_OTP) {

                PhoneInputStep(
                    phoneInput = phoneInput,
                    onPhoneChange = { phoneInput = it },
                    onSendOtp = { onSendOtp(phoneInput) },
                    isLoading = uiState is LoginUiState.SendingOtp,
                    isError = uiState is LoginUiState.Error && uiState.currentPhase == LoginUiState.Error.Phase.SENDING_OTP,
                )
            }

            // ── Step 2: OTP input ─────────────────────────────────────────
            if (uiState is LoginUiState.OtpSent || 
                uiState is LoginUiState.VerifyingOtp ||
                uiState is LoginUiState.Error && uiState.currentPhase == LoginUiState.Error.Phase.VERIFYING_OTP) {

                val otpSentState = if (uiState is LoginUiState.OtpSent) uiState 
                                   else if (uiState is LoginUiState.Error) LoginUiState.OtpSent(phone = "") 
                                   else LoginUiState.OtpSent(phone = "")

                OtpInputStep(
                    phoneDisplay = (uiState as? LoginUiState.OtpSent)?.phone ?: "",
                    otpInput = otpInput,
                    onOtpChange = { otpInput = it },
                    onVerifyOtp = { onVerifyOtp(otpInput) },
                    onResendOtp = { onResendOtp() },
                    resendCooldownSeconds = otpSentState.resendCooldownSeconds,
                    isLoading = uiState is LoginUiState.VerifyingOtp,
                    isError = uiState is LoginUiState.Error && uiState.currentPhase == LoginUiState.Error.Phase.VERIFYING_OTP,
                )
            }

            // ── Loading indicator during verification ─────────────────────
            if (uiState is LoginUiState.VerificationSuccess) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Completing setup...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // ── Snackbar for errors ───────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

// ── Phone input step ──────────────────────────────────────────────────────────

@Composable
private fun PhoneInputStep(
    phoneInput: String,
    onPhoneChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    isLoading: Boolean,
    isError: Boolean,
) {
    OutlinedTextField(
        value         = phoneInput,
        onValueChange = { if (it.length <= 10) onPhoneChange(it) },
        label         = { Text("Mobile number") },
        placeholder   = { Text("10-digit number") },
        helperText    = if (isError) { { Text("Invalid number or network error", color = MaterialTheme.colorScheme.error) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        isError       = isError,
    )

    Spacer(Modifier.height(16.dp))

    Button(
        onClick  = onSendOtp,
        modifier = Modifier.fillMaxWidth(),
        enabled  = phoneInput.length == 10 && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterVertically),
                strokeWidth = 2.dp,
                color    = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text("Send OTP")
        }
    }
}

// ── OTP input step ────────────────────────────────────────────────────────────

@Composable
private fun OtpInputStep(
    phoneDisplay: String,
    otpInput: String,
    onOtpChange: (String) -> Unit,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit,
    resendCooldownSeconds: Int,
    isLoading: Boolean,
    isError: Boolean,
) {
    Text(
        text  = "OTP sent to +91-${phoneDisplay.takeLast(10)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value         = otpInput,
        onValueChange = { if (it.length <= 6) onOtpChange(it) },
        label         = { Text("Enter OTP") },
        placeholder   = { Text("6-digit code") },
        helperText    = if (isError) { { Text("Invalid OTP. Check and try again.", color = MaterialTheme.colorScheme.error) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        isError       = isError,
    )

    Spacer(Modifier.height(16.dp))

    Button(
        onClick  = onVerifyOtp,
        modifier = Modifier.fillMaxWidth(),
        enabled  = otpInput.length == 6 && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterVertically),
                strokeWidth = 2.dp,
                color    = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text("Verify OTP")
        }
    }

    Spacer(Modifier.height(8.dp))

    // ── Resend button with cooldown ─────────────────────────────────────
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text  = "Didn't receive OTP?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        if (resendCooldownSeconds > 0) {
            Text(
                text  = "Resend in ${resendCooldownSeconds}s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TextButton(onClick = onResendOtp) {
                Text("Resend OTP")
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun LoginScreenIdlePreview() {
    DeliveryAgentTheme { LoginScreen(uiState = LoginUiState.Idle) }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenOtpSentPreview() {
    DeliveryAgentTheme {
        LoginScreen(
            uiState = LoginUiState.OtpSent(phone = "+919876543210", resendCooldownSeconds = 15),
        )
    }
}
