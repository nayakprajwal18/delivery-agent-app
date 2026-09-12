package com.yourteam.deliveryagent.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourteam.deliveryagent.data.model.Order
import com.yourteam.deliveryagent.ui.screens.dashboard.DashboardUiState
import com.yourteam.deliveryagent.ui.theme.DeliveryAgentTheme

/**
 * Home / Dashboard screen showing agent's real-time stats and active delivery.
 *
 * Displays:
 * - Greeting with agent name
 * - Availability toggle (Online/Offline)
 * - Stats cards: available deliveries, completed today, earnings today
 * - Active delivery card (if assigned) or CTA to browse deliveries
 *
 * Data updates in real-time via Supabase Realtime subscriptions.
 */
@Composable
fun HomeScreen(
    uiState: DashboardUiState,
    onNavigateToAvailableDeliveries: () -> Unit = {},
    onNavigateToDeliveryDetails: (orderId: String) -> Unit = {},
    onToggleAvailability: (isOnline: Boolean) -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background,
    ) {
        when (uiState) {
            is DashboardUiState.Loading -> {
                LoadingState()
            }

            is DashboardUiState.Success -> {
                SuccessState(
                    uiState                         = uiState,
                    onNavigateToAvailableDeliveries = onNavigateToAvailableDeliveries,
                    onNavigateToDeliveryDetails     = onNavigateToDeliveryDetails,
                    onToggleAvailability            = onToggleAvailability,
                    onRefresh                       = onRefresh,
                )
            }

            is DashboardUiState.Error -> {
                ErrorState(
                    message  = uiState.message,
                    onRetry  = onRefresh,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Loading dashboard...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text  = "Failed to load dashboard",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun SuccessState(
    uiState: DashboardUiState.Success,
    onNavigateToAvailableDeliveries: () -> Unit,
    onNavigateToDeliveryDetails: (orderId: String) -> Unit,
    onToggleAvailability: (isOnline: Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header with greeting & availability toggle ──────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text  = "Good morning, ${uiState.profile.fullName}! 👋",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text  = if (uiState.agent.isOnline) "You're Online" else "You're Offline",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.agent.isOnline)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Availability toggle
            Switch(
                checked         = uiState.agent.isOnline,
                onCheckedChange = onToggleAvailability,
                enabled         = !uiState.isTogglingAvailability,
            )
        }

        // ── Stats row ─────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label    = "Available",
                value    = uiState.availableDeliveriesCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = "Completed",
                value    = uiState.todayCompletedCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label    = "Earnings",
                value    = "₹${uiState.todayEarnings.toInt()}",
                modifier = Modifier.weight(1f),
            )
        }

        // ── Active delivery or CTA ────────────────────────────────────────
        Text("Active Delivery", style = MaterialTheme.typography.titleMedium)

        if (uiState.activeOrder != null) {
            ActiveDeliveryCard(
                order      = uiState.activeOrder,
                onTap      = { onNavigateToDeliveryDetails(uiState.activeOrder.id) },
            )
        } else {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(
                    modifier            = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.DeliveryDining,
                            contentDescription = "No active delivery",
                            modifier           = Modifier.align(Alignment.CenterVertically),
                            tint               = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text  = "No active delivery right now",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Button(
                        onClick  = onNavigateToAvailableDeliveries,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Browse Available Deliveries")
                    }
                }
            }
        }

        // ── Last update error (if any) ────────────────────────────────────
        if (uiState.lastUpdateError != null) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text  = uiState.lastUpdateError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActiveDeliveryCard(
    order: Order,
    onTap: () -> Unit,
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text  = order.shopName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text  = order.dropAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Distance", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text  = "${order.distanceKm ?: "—"} km",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Column {
                        Text("Earnings", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text  = "₹${order.deliveryFee.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                TextButton(onClick = onTap) {
                    Text("Details →")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    DeliveryAgentTheme {
        HomeScreen(
            uiState = DashboardUiState.Loading,
        )
    }
}
