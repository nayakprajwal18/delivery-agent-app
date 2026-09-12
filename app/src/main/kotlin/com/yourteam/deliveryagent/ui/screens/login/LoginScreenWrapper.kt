package com.yourteam.deliveryagent.ui.screens.login

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourteam.deliveryagent.DeliveryAgentApp
import com.yourteam.deliveryagent.ui.screens.LoginScreen

/**
 * Wrapper composable that creates the LoginViewModel with proper dependency injection
 * and wires it to the LoginScreen.
 *
 * This is called from AppNavGraph → composable(NavRoutes.LOGIN).
 */
@Composable
fun LoginScreenWrapper(
    onLoginSuccess: () -> Unit = {},
) {
    val context = LocalContext.current
    val container = (context.applicationContext as DeliveryAgentApp).container

    // Create ViewModel using a factory
    val viewModel = viewModel<LoginViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(
                    container.authRepository,
                    container.agentRepository,
                ) as T
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    LoginScreen(
        uiState = uiState,
        onSendOtp = { phoneInput -> viewModel.sendOtp(phoneInput) },
        onVerifyOtp = { otp -> viewModel.verifyOtp(otp) },
        onResendOtp = { viewModel.resendOtp() },
        onDismissError = { viewModel.dismissError() },
        onLoginSuccess = onLoginSuccess,
    )
}
