package com.yourteam.deliveryagent.ui.screens.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.yourteam.deliveryagent.core.SupabaseClientProvider
import com.yourteam.deliveryagent.data.repository.AgentRepository
import com.yourteam.deliveryagent.data.repository.AuthRepository
import com.yourteam.deliveryagent.ui.screens.ProfileScreen

/**
 * Wrapper composable that instantiates [ProfileViewModel] with DI,
 * collects UI state, and renders [ProfileScreen].
 */
@Composable
fun ProfileScreenWrapper(
    onLogout: () -> Unit = {},
) {
    // Instantiate repositories with DI
    val supabase = SupabaseClientProvider.client
    val authRepository = AuthRepository(supabase)
    val agentRepository = AgentRepository(supabase)

    val viewModel = remember {
        ProfileViewModel(
            authRepository = authRepository,
            agentRepository = agentRepository,
        )
    }

    val uiState = viewModel.uiState.collectAsState().value

    // Observe UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                ProfileUiEvent.NavigateToLogin -> onLogout()
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        onFullNameChange = { viewModel.updateFullName(it) },
        onVehicleTypeChange = { viewModel.updateVehicleType(it) },
        onVehicleNumberChange = { viewModel.updateVehicleNumber(it) },
        onAvailabilityChange = { viewModel.toggleAvailability(it) },
        onSave = { viewModel.saveProfile() },
        onLogout = { viewModel.logout() },
        onRetry = { viewModel.retry() },
    )
}
